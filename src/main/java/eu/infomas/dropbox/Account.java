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
import java.util.Map;

import static eu.infomas.dropbox.Utils.asLong;
import static eu.infomas.dropbox.Utils.asString;

/**
 * Information about a user's account.
 *
 * @author Original Author is Dropbox
 * @author <a href="mailto:rmuller@xiam.nl">Ronald K. Muller</a> (small modifications)
 * @since infomas-asl 3.0.2
 */
public final class Account implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String country;
    private final String displayName;
    private final long quota;
    private final long quotaNormal;
    private final long quotaShared;
    private final long uid;
    private final String referralLink;

    /**
     * Creates an account from a Map.
     *
     * @param jsonMap a Map that looks like:
     * <pre>
     * {"country": "",
     *  "display_name": "John Q. User",
     *  "quota_info": {
     *    "shared": 37378890,
     *    "quota": 62277025792,
     *    "normal": 263758550
     *   },
     *  "uid": "174"}
     * </pre>
     */
    private Account(Map<String, Object> jsonMap) {
        country = asString(jsonMap, "country");
        displayName = asString(jsonMap, "display_name");
        uid = asLong(jsonMap, "uid");
        referralLink = asString(jsonMap, "referral_link");

        final Map<String, Object> quotamap = (Map<String, Object>)jsonMap.get("quota_info");
        quota = asLong(quotamap, "quota");
        quotaNormal = asLong(quotamap, "normal");
        quotaShared = asLong(quotamap, "shared");
    }

    static Account valueOf(final Map<String, Object> jsonMap) {
        return jsonMap == null ? null : new Account(jsonMap);
    }

    /**
     * The user's ISO country code.
     */
    public String getCountry() {
        return country;
    }

    /**
     * The user's "real" name.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * The user's quota, in bytes.
     */
    public long getQuota() {
        return quota;
    }

    /**
     * The user's quota excluding shared files.
     */
    public long getQuotaNormal() {
        return quotaNormal;
    }

    /**
     * The user's quota of shared files.
     */
    public long getQuotaShared() {
        return quotaShared;
    }

    /**
     * The user's account ID.
     */
    public long getUid() {
        return uid;
    }

    /**
     * The url the user can give to get referral credit.
     */
    public String getReferralLink() {
        return referralLink;
    }

    /**
     * Return a human String with all data hold by this instance.
     * Only for debugging.
     */
    @Override
    public String toString() {
        return "Account{" + "country=" + country + ", displayName=" + displayName +
            ", quota=" + quota + ", quotaNormal=" + quotaNormal +
            ", quotaShared=" + quotaShared + ", uid=" + uid +
            ", referralLink=" + referralLink + '}';
    }

}
