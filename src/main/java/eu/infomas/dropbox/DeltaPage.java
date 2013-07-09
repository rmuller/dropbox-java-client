/* SUPPRESS CHECKSTYLE RegexpHeader
 *
 * Copyright (c) 2009-2011 Dropbox, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package eu.infomas.dropbox;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static eu.infomas.dropbox.Utils.*;

/**
 * A page of {@link DeltaEntry}s, returned by {@link Dropbox#delta(java.lang.String)}.
 *
 * @author Original Author is Dropbox
 * @author <a href="mailto:rmuller@xiam.nl">Ronald K. Muller</a> (refactoring)
 * @since infomas-asl 3.0.2
 */
public final class DeltaPage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String cursor;
    private final boolean reset;
    private final List<DeltaEntry> entries;
    private final boolean hasMore;

    DeltaPage(final Map<String, Object> jsonMap) {
        reset = asBoolean(jsonMap, "reset");
        cursor = asString(jsonMap, "cursor");
        hasMore = asBoolean(jsonMap, "hasMore");
        final List<List<Object>> list = (List<List<Object>>) jsonMap.get("entries");
        entries = new ArrayList<DeltaEntry>(list.size());
        for (final List<Object> entryList : list) {
            entries.add(new DeltaEntry(entryList));
        }
    }

    /**
     * A string that is used to keep track of your current state. 
     * On the next call to {@link Dropbox#delta(java.lang.String) }, 
     * pass in this value to pick up where you left off.
     */
    public String getCursor() {
        return cursor;
    }

    /**
     * If <code>true</code>, then you should reset your local state to be an empty folder
     * before processing the list of delta entries. This is only <code>true</code> in 
     * rare situations.
     */
    public boolean isReset() {
        return reset;
    }

    /**
     * Apply these entries to your local state to catch up with the Dropbox server's
     * state.
     */
    public List<DeltaEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    /**
     * If <code>true</code>, then there are more entries available; you can call {@link
     * Dropbox#delta(java.lang.String) } again immediately to retrieve those entries. If
     * <code>false</code>, then wait at least 5 minutes (preferably longer) before
     * checking again.
     */
    public boolean hasMore() {
        return hasMore;
    }
    
}
