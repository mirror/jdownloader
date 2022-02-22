package jd.config;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.appwork.utils.StringUtils;

public class WeakStringCache {
    private ReferenceQueue<String> queue = new ReferenceQueue<String>();

    protected class WeakStringCacheEntry extends WeakReference<String> {
        protected final long entryHash;

        protected WeakStringCacheEntry(String referent) {
            super(referent, queue);
            entryHash = WeakStringCache.this.entryHash(referent);
        }

        protected WeakStringCacheEntry(String referent, final long entryHash) {
            super(referent, queue);
            this.entryHash = entryHash;
        }

        @Override
        public String toString() {
            return String.valueOf(entryHash);
        }

        protected int getStringLength() {
            return WeakStringCache.this.getStringLength(entryHash);
        }
    }

    protected class WeakStringCacheEntries extends ArrayList<WeakStringCacheEntry> {
        protected final int stringLength;

        protected WeakStringCacheEntries(int stringLength) {
            this.stringLength = stringLength;
        }
    }

    protected long entryHash(final String string) {
        final int hash = string.hashCode();
        final int length = string.length();
        final long ret = (((long) hash) << 32) | (length & 0xffffffffL);
        return ret;
    }

    protected int getStringHash(long entryHash) {
        return (int) (entryHash >> 32);
    }

    protected int getStringLength(long entryHash) {
        return (int) entryHash;
    }

    private List<WeakStringCacheEntries> entries   = new ArrayList<WeakStringCacheEntries>();
    private long                         cacheSize = 0;

    protected int compare(long x, long y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

    private WeakStringCacheEntries getWeakStringCacheEntries(final int stringLength) {
        int min = 0;
        int max = entries.size() - 1;
        int mid = 0;
        while (min <= max) {
            mid = (max + min) / 2;
            final WeakStringCacheEntries midEntry = entries.get(mid);
            final int comp = compare(stringLength, midEntry.stringLength);
            if (min == max) {
                if (stringLength == midEntry.stringLength) {
                    return midEntry;
                } else {
                    break;
                }
            }
            if (comp < 0) {
                // searchFor is smaller
                max = mid;
            } else if (comp > 0) {
                // searchFor is larger
                min = mid + 1;
            } else {
                return midEntry;
            }
        }
        final WeakStringCacheEntries ret;
        entries.add(ret = new WeakStringCacheEntries(stringLength));
        Collections.sort(entries, new Comparator<WeakStringCacheEntries>() {
            @Override
            public int compare(WeakStringCacheEntries o1, WeakStringCacheEntries o2) {
                return WeakStringCache.this.compare(o1.stringLength, o2.stringLength);
            }
        });
        return ret;
    }

    public String cache(final String string) {
        clean();
        final long searchFor = entryHash(string);
        final int searchForLength = string.length();
        final WeakStringCacheEntries entries = getWeakStringCacheEntries(searchForLength);
        int min = 0;
        int max = entries.size() - 1;
        int mid = 0;
        // int search = 0;
        int next = 0;
        while (min <= max) {
            mid = (max + min) / 2;
            WeakStringCacheEntry midEntry = entries.get(mid);
            final int comp = compare(searchFor, midEntry.entryHash);
            // search++;
            if (min == max) {
                final String midValue = midEntry.get();
                if (StringUtils.equals(string, midValue)) {
                    // System.out.println("hit a \t" + search + " size:" + entries.size() + "/" + cacheSize + " length:" + searchForLength);
                    return midValue;
                } else {
                    // System.out.println("add a \t" + search + "\t" + next + " size:" + entries.size() + "/" + cacheSize + " length:" +
                    // searchForLength);
                    break;
                }
            }
            if (comp < 0) {
                // searchFor is smaller
                max = mid;
            } else if (comp > 0) {
                // searchFor is larger
                min = mid + 1;
            } else {
                // searchFor matches
                while (mid <= max && midEntry.entryHash == searchFor) {
                    midEntry = entries.get(mid++);
                    final String midValue = midEntry.get();
                    if (StringUtils.equals(string, midValue)) {
                        // System.out.println("hit b \t" + search + "\t" + next + " size:" + entries.size() + "/" + cacheSize + " length:" +
                        // searchForLength);
                        return midValue;
                    } else {
                        next = next++;
                    }
                }
                break;
            }
        }
        // System.out.println("add b \t" + search + "\t" + next + " size:" + entries.size() + "/" + cacheSize + " length:" +
        // searchForLength);
        cacheSize = cacheSize + 1;
        entries.add(min, new WeakStringCacheEntry(string, searchFor));
        Collections.sort(entries, new Comparator<WeakStringCacheEntry>() {
            @Override
            public int compare(WeakStringCacheEntry o1, WeakStringCacheEntry o2) {
                return WeakStringCache.this.compare(o1.entryHash, o2.entryHash);
            }
        });
        return string;
    }

    private void clean() {
        Reference<? extends String> purge;
        while ((purge = this.queue.poll()) != null) {
            final WeakStringCacheEntry entry = (WeakStringCacheEntry) purge;
            final Iterator<WeakStringCacheEntry> it = getWeakStringCacheEntries(entry.getStringLength()).iterator();
            while (it.hasNext()) {
                final WeakStringCacheEntry next = it.next();
                if (purge == next) {
                    cacheSize = cacheSize - 1;
                    //
                    it.remove();
                    break;
                }
            }
        }
    }
}
