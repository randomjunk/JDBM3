/*******************************************************************************
 * Copyright 2010 Cees De Groot, Alex Boisvert, Jan Kotek
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package jdbm;

import java.io.IOError;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

/**
 * Abstract class for record manager which implements most of stuff
 * 
 * @author Jan Kotek
 *
 */
abstract class RecordManagerImpl implements RecordManager{
	
    public <K, V> PrimaryHashMap<K, V> createHashMap(String name) {
        return createHashMap(name, null, null);
    }


	public synchronized <K, V> PrimaryHashMap<K, V> createHashMap(String name, Serializer<K> keySerializer, Serializer<V> valueSerializer) {
		try{
			HTree<K, V> tree = null;
        
			long recid = getNamedObject( name);
			if ( recid != 0 ) {
				tree = new HTree( this, recid, keySerializer, valueSerializer);
			} else {
				tree = new HTree(this, keySerializer, valueSerializer);
				setNamedObject( name, tree.getRecid() );
			}
			return tree;
		}catch(IOException  e){
			throw new IOError(e);
		}
	}

        public synchronized <K> Set<K> createHashSet(String name) {
            return createHashSet(name,null);
        }

        public synchronized <K> Set<K> createHashSet(String name, Serializer<K> keySerializer) {
                return new HTreeSet(createHashMap(name,keySerializer,null));
	}

	@SuppressWarnings("unchecked")
	public <K extends Comparable, V> PrimaryTreeMap<K, V> createTreeMap(String name) {
		return createTreeMap(name, null,null,null);
	}



	public synchronized <K, V> PrimaryTreeMap<K, V> createTreeMap(String name,
                Comparator<K> keyComparator, Serializer<V> valueSerializer, Serializer<K> keySerializer) {
		try{
			BTree<K,V> tree = null;
        
			// create or load 
			long recid = getNamedObject( name);
			if ( recid != 0 ) {
				tree = BTree.load( this, recid );
			} else {
				tree = BTree.createInstance(this,keyComparator);
				setNamedObject( name, tree.getRecid() );
			}
			tree.setKeySerializer(keySerializer);
			tree.setValueSerializer(valueSerializer);
			
			return tree.asMap();
		}catch(IOException  e){
			throw new IOError(e);
		}	
	}


        public synchronized <K> SortedSet<K> createTreeSet(String name) {
            return createTreeSet(name, null, null);
        }


        public synchronized <K> SortedSet<K> createTreeSet(String name, Comparator<K> keyComparator,Serializer<K> keySerializer) {
            return new BTreeSet<K>(createTreeMap(name,
                    keyComparator != null ? keyComparator : ComparableComparator.INSTANCE,
                    null, keySerializer));
        }


    public <K> List<K> createLinkedList(String name){
        return createLinkedList(name, null);
    }

    public <K> List<K> createLinkedList(String name, Serializer<K> serializer){
        try{
            JDBMLinkedList<K> list;

            // create or load
            long recid = getNamedObject( name);
            if ( recid != 0 ) {
                list = (JDBMLinkedList<K>) fetch(recid);
                list.setRecmanAndListRedic(this, recid);
            } else {
                //allocate record and overwrite it
                recid = insert(null);
                list = new JDBMLinkedList<K>(this,recid);
                update(recid,list);
                setNamedObject( name, recid );
            }
            return list;
        }catch (IOException e){
            throw new IOError(e);
        }

    }




    public void update( long recid, Object obj ) throws IOException{
    	update( recid, obj, defaultSerializer() );
    }
    
    public Object fetch( long recid ) throws IOException{
    	return fetch( recid, defaultSerializer() );
    }

    public long insert( Object obj )throws IOException{
    	return insert( obj, defaultSerializer() );
    }


	
}