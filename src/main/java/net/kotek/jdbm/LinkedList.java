/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.kotek.jdbm;


import java.io.*;
import java.util.*;

/**
 * LinkedList which stores its nodes on disk.
 *
 * @author Jan Kotek
 */
class LinkedList<E> extends AbstractSequentialList<E> {

    private DBAbstract db;

    private long rootRecid;
    
    static private final class Root{
        long first;
        long last;
        long size;
    }

    private static final Serializer<Root> ROOT_SERIALIZER= new Serializer<Root>(){

        public void serialize(DataOutput out, Root obj) throws IOException {
            LongPacker.packLong(out,obj.first);
            LongPacker.packLong(out,obj.last);
            LongPacker.packLong(out,obj.size);
        }

        public Root deserialize(DataInput in) throws IOException, ClassNotFoundException {
            Root r = new Root();
            r.first = LongPacker.unpackLong(in);
            r.last = LongPacker.unpackLong(in);
            r.size = LongPacker.unpackLong(in);
            return r;
        }
    };

    private Serializer<E> valueSerializer;

    /**
     * indicates that entry values should not be loaded during deserialization, used during defragmentation
     */
    protected boolean loadValues = true;

    /** constructor used for deserialization */
    LinkedList(long rootRecid, Serializer<E> valueSerializer) {
        this.rootRecid = rootRecid;
        this.valueSerializer = valueSerializer;
    }

    /** constructor used to create new empty list*/
    LinkedList(DBAbstract db, Serializer<E> valueSerializer) throws IOException {
        this.db = db;
        if (valueSerializer != null && !(valueSerializer instanceof Serializable))
            throw new IllegalArgumentException("Serializer does not implement Serializable");
        this.valueSerializer = valueSerializer;
        //create root
        this.rootRecid = db.insert(new Root(), ROOT_SERIALIZER);
    }

    void setPersistenceContext(DBAbstract db) {
        this.db = db;
    }


    public ListIterator<E> listIterator(int index) {
        Root r = getRoot();
        if (index < 0 || index > r.size)
            throw new IndexOutOfBoundsException();


        Iter iter = new Iter();
        iter.next = r.first;


        //scroll to requested position
        //TODO scroll from end, if beyond half
        for (int i = 0; i < index; i++) {
            iter.next();
        }
        return iter;

    }

    private Root getRoot(){
        try {
           return db.fetch(rootRecid,ROOT_SERIALIZER);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }


    public int size() {
        return (int) getRoot().size;
    }

    public boolean add(Object value) {
        try {                           
            Root r = getRoot();
            Entry e = new Entry(r.last, 0, value);
            long recid = db.insert(e, entrySerializer);

            //update old last Entry to point to new record
            if (r.last != 0) {
                Entry oldLast = db.fetch(r.last, entrySerializer);
                if (oldLast.next != 0) throw new Error();
                oldLast.next = recid;
                db.update(r.last, oldLast, entrySerializer);
            }

            //update linked list
            r.last = recid;
            if (r.first == 0) r.first = recid;
            r.size++;
            db.update(rootRecid, r, ROOT_SERIALIZER);
            modCount++;
            return true;
        } catch (IOException e) {
            throw new IOError(e);
        }

    }

    private Entry<E> fetch(long recid) {
        try {
            return db.fetch(recid, entrySerializer);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    /**
     * called from Serialization object
     */
    static LinkedList deserialize(DataInput is, Serializer ser) throws IOException, ClassNotFoundException {
        long rootrecid = LongPacker.unpackLong(is);
        Serializer serializer = (Serializer)  ser.deserialize(is);
        return new LinkedList(rootrecid, serializer);
    }

    void serialize(DataOutput out) throws IOException {
        LongPacker.packLong(out, rootRecid);
        db.defaultSerializer().serialize(out, valueSerializer);
    }

    private final Serializer<Entry> entrySerializer = new Serializer<Entry>() {

        public void serialize(DataOutput out, Entry e) throws IOException {
            LongPacker.packLong(out, e.prev);
            LongPacker.packLong(out, e.next);
            if (valueSerializer != null)
                valueSerializer.serialize(out, (E) e.value);
            else
                db.defaultSerializer().serialize(out, e.value);
        }

        public Entry<E> deserialize(DataInput in) throws IOException, ClassNotFoundException {
            long prev = LongPacker.unpackLong(in);
            long next = LongPacker.unpackLong(in);
            Object value = null;
            if (loadValues)
                value = valueSerializer == null ? db.defaultSerializer().deserialize(in) : valueSerializer.deserialize(in);
            return new LinkedList.Entry(prev, next, value);
        }
    };

    static class Entry<E> {
        long prev = 0;
        long next = 0;

        E value;

        public Entry(long prev, long next, E value) {
            this.prev = prev;
            this.next = next;
            this.value = value;
        }
    }

    private final class Iter implements ListIterator<E> {

        private int expectedModCount = modCount;
        private int index = 0;

        private long prev = 0;
        private long next = 0;

        private byte lastOper = 0;

        public boolean hasNext() {
            return next != 0;
        }


        public E next() {
            if (next == 0) throw new NoSuchElementException();
            checkForComodification();

            Entry<E> e = fetch(next);

            prev = next;
            next = e.next;
            index++;
            lastOper = +1;
            return e.value;
        }

        public boolean hasPrevious() {
            return prev != 0;
        }

        public E previous() {
            checkForComodification();
            Entry<E> e = fetch(prev);
            next = prev;
            prev = e.prev;
            index--;
            lastOper = -1;
            return e.value;
        }

        public int nextIndex() {
            return index;
        }

        public int previousIndex() {
            return index - 1;
        }

        public void remove() {
            checkForComodification();
            try {
                if (lastOper == 1) {
                    //last operation was next() so remove previous element
                    lastOper = 0;

                    Entry<E> p = db.fetch(prev, entrySerializer);
                    //update entry before previous
                    if (p.prev != 0) {
                        Entry<E> pp = db.fetch(p.prev, entrySerializer);
                        pp.next = p.next;
                        db.update(p.prev, pp, entrySerializer);
                    }
                    //update entry after next
                    if (p.next != 0) {
                        Entry<E> pn = db.fetch(p.next, entrySerializer);
                        pn.prev = p.prev;
                        db.update(p.next, pn, entrySerializer);
                    }
                    //remove old record from db
                    db.delete(prev);
                    //update list
                    Root r = getRoot();
                    if (r.first == prev)
                        r.first = next;
                    if (r.last == prev)
                        r.last = next;
                    r.size--;
                    db.update(rootRecid, r,ROOT_SERIALIZER);
                    modCount++;
                    expectedModCount++;
                    //update iterator
                    prev = p.prev;

                } else if (lastOper == -1) {
                    //last operation was prev() so remove next element
                    lastOper = 0;

                    Entry<E> n = db.fetch(next, entrySerializer);
                    //update entry before next
                    if (n.prev != 0) {
                        Entry<E> pp = db.fetch(n.prev, entrySerializer);
                        pp.next = n.next;
                        db.update(n.prev, pp, entrySerializer);
                    }
                    //update entry after previous
                    if (n.next != 0) {
                        Entry<E> pn = db.fetch(n.next, entrySerializer);
                        pn.prev = n.prev;
                        db.update(n.next, pn, entrySerializer);
                    }
                    //remove old record from db
                    db.delete(next);
                    //update list
                    Root r = getRoot();
                    if (r.last == next)
                        r.last = prev;
                    if (r.first == next)
                        r.first = prev;
                    r.size--;
                    db.update(rootRecid, r,ROOT_SERIALIZER);
                    modCount++;
                    expectedModCount++;
                    //update iterator
                    next = n.next;

                } else
                    throw new IllegalStateException();
            } catch (IOException e) {
                throw new IOError(e);
            }
        }

        public void set(E value) {
            checkForComodification();
            try {
                if (lastOper == 1) {
                    //last operation was next(), so update previous item
                    lastOper = 0;
                    Entry<E> n = db.fetch(prev, entrySerializer);
                    n.value = value;
                    db.update(prev, n, entrySerializer);
                } else if (lastOper == -1) {
                    //last operation was prev() so update next item
                    lastOper = 0;
                    Entry<E> n = db.fetch(next, entrySerializer);
                    n.value = value;
                    db.update(next, n, entrySerializer);
                } else
                    throw new IllegalStateException();
            } catch (IOException e) {
                throw new IOError(e);
            }

        }

        public void add(E value) {
            checkForComodification();
            //use more efficient method if possible
            if (next == 0) {
                LinkedList.this.add(value);
                expectedModCount++;
                return;
            }
            try {
                //insert new entry
                Entry<E> e = new Entry<E>(prev, next, value);
                long recid = db.insert(e, entrySerializer);

                //update previous entry
                if (prev != 0) {
                    Entry<E> p = db.fetch(prev, entrySerializer);
                    if (p.next != next) throw new Error();
                    p.next = recid;
                    db.update(prev, p, entrySerializer);
                }

                //update next entry
                Entry<E> n = fetch(next);
                if (n.prev != prev) throw new Error();
                n.prev = recid;
                db.update(next, n, entrySerializer);

                //update List
                Root r = getRoot();
                r.size++;
                db.update(rootRecid, r, ROOT_SERIALIZER);

                //update iterator
                expectedModCount++;
                modCount++;
                prev = recid;

            } catch (IOException e) {
                throw new IOError(e);
            }

        }

        final void checkForComodification() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
        }
    }

    /**
     * Copyes collection from one db to other, while keeping logical recids unchanged
     */
    static void defrag(long recid, DBStore r1, DBStore r2) throws IOException {
        try {
            //move linked list itself
            byte[] data = r1.fetchRaw(recid);
            r2.forceInsert(recid, data);
            DataInputOutput in = new DataInputOutput();
            in.reset(data);
            LinkedList l = (LinkedList) r1.defaultSerializer().deserialize(in);
            l.loadValues = false;
            //move linkedlist root
            if(l.rootRecid == 0) //empty list, done
                return;

            data = r1.fetchRaw(l.rootRecid);
            r2.forceInsert(l.rootRecid, data);
            in.reset(data);
            Root r = ROOT_SERIALIZER.deserialize(in);
            //move all other nodes in linked list
            long current = r.first;
            while (current != 0) {
                data = r1.fetchRaw(current);
                in.reset(data);
                r2.forceInsert(current, data);

                Entry e = (Entry) l.entrySerializer.deserialize(in);
                current = e.next;
            }
        } catch (ClassNotFoundException e) {
            throw new IOError(e);
        }

    }

}