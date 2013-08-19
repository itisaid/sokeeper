package com.hbd.cmdb.search;

import java.util.Map.Entry;

public class CmdbEntry<K, V> implements Entry<K, V> {

	K key;
	V value;

	public K getKey() {
		return key;
	}

	public void setKey(K key) {
		this.key = key;
	}

	public V getValue() {
		return value;
	}

	public V setValue(V value) {
		this.value = value;
		return value;
	}

}
