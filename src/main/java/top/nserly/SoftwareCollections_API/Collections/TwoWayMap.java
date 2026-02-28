/*
 * Copyright 2026 PicturePlayer;Nserly
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package top.nserly.SoftwareCollections_API.Collections;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TwoWayMap<K, V> {
    // 正向映射：key→value
    private final Map<K, V> keyToValue = new HashMap<>();
    // 反向映射：value→key（需保证value唯一）
    private final Map<V, K> valueToKey = new HashMap<>();

    // 添加键值对（若value已存在，会覆盖旧映射）
    public void put(K key, V value) {
        // 先移除旧映射（若存在）
        if (keyToValue.containsKey(key)) {
            V oldValue = keyToValue.get(key);
            valueToKey.remove(oldValue);
        }
        if (valueToKey.containsKey(value)) {
            K oldKey = valueToKey.get(value);
            keyToValue.remove(oldKey);
        }

        // 添加新映射
        keyToValue.put(key, value);
        valueToKey.put(value, key);
    }

    // 从key获取value
    public V getValue(K key) {
        return keyToValue.get(key);
    }

    // 从value获取key
    public K getKey(V value) {
        return valueToKey.get(value);
    }

    // 移除映射
    public void removeByKey(K key) {
        V value = keyToValue.remove(key);
        if (value != null) {
            valueToKey.remove(value);
        }
    }

    // 获取所有value
    public Collection<V> values() {
        return keyToValue.values();
    }

    public Set<V> valueSet() {
        return valueToKey.keySet();
    }

    //获取所有key
    public Collection<K> keys() {
        return valueToKey.values();
    }

    public Set<K> keySet() {
        return keyToValue.keySet();
    }


    //清除
    public void clear() {
        keyToValue.clear();
        valueToKey.clear();
    }

    public boolean isEmpty() {
        return keyToValue.isEmpty() || valueToKey.isEmpty();
    }
}