/*
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

import java.util.List;
import java.util.Map;

/**
 * A single entry in a {@link DeltaPage}.
 *
 * @author Original Author is Dropbox
 * @author <a href="mailto:rmuller@xiam.nl">Ronald K. Muller</a> (refactoring)
 * @since infomas-asl 3.0.2
 */
public final class DeltaEntry {

    private final String lcPath;
    private final Entry metadata;

    DeltaEntry(final List<Object> jsonList) {
        this.lcPath = (String) jsonList.get(0);
        this.metadata = Entry.valueOf((Map) jsonList.get(1));
    }

    /**
     * The lower-cased path of the entry. 
     * Dropbox compares file paths in a case-insensitive manner. For example, 
     * an entry for <code>"/readme.txt"</code> should overwrite the entry for
     * <code>"/ReadMe.TXT"</code>.
     * <br/>
     * To get the original case-preserved path, look in the {@link #metadata metadata}
     * field.
     */
    public String getLowerCasedPath() {
        return lcPath;
    }

    /**
     * If this is
     * <code>null</code>, it means that this path doesn't exist on on Dropbox's copy of
     * the file system. To update your local state to match, delete whatever is at that
     * path, including any children. If your local state doesn't have anything at this
     * path, ignore this entry.
     * <br/>
     * If this is not <code>null</code>, it means that Dropbox has a file/folder at this 
     * path with the given metadata. To update your local state to match, add the entry 
     * to your local state as well.
     * <ul> 
     * <li> If the path refers to parent folders that don't exist
     * yet in your local state, create those parent folders in your local state.</li>
     * <li> If the metadata is for a file, replace whatever your local state has at that
     * path with the new entry.</li>
     * <li> If the metadata is for a folder, check what your local state has at the path. 
     * If it's a file, replace it with the new entry. If it's a folder, apply the new 
     * metadata to the folder, but do not modify the folder's
     * children.</li>
     * </ul>
     */
    public Entry getMetadata() {
        return metadata;
    }
}
