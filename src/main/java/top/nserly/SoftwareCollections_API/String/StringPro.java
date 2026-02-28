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

package top.nserly.SoftwareCollections_API.String;

import java.util.ArrayList;
import java.util.Collections;

public final class StringPro implements Cloneable {
    private StringBuffer str;


    public void append(Object object) {
        str.append(object.toString());
    }

    public StringPro(String string) {
        str = new StringBuffer(string);
    }

    public void clear() {
        str = new StringBuffer();
    }

    public StringPro() {
        str = new StringBuffer();
    }

    public StringPro(StringPro stringPro) {
        str = new StringBuffer(stringPro.toString());
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public static ArrayList<String> StringToList(String... strings) {
        ArrayList<String> arrayList = new ArrayList<>();
        Collections.addAll(arrayList, strings);
        return arrayList;
    }

    public void appendLn(Object object) {
        str.append(object).append("\n");
    }

    public String toString() {
        return str.toString();
    }

    public StringBuffer toStringBuffer() {
        return str;
    }

    public int hashCode() {
        return str.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        StringBuffer str = ((StringPro) object).toStringBuffer();
        return this.toString().contentEquals(str);
    }
}
