package pl.mkamola.kthttp2.net.http2

class BitBuffer {
    var bitsInOctet = 0

    fun readBit(): Int {
        return 1
    }

    fun readByte(): Int {
        return 0
    }

    fun readInt(): Int {
        return 0
    }

    fun readBytes(len: Int): ByteArray {
        return ByteArray(len)
    }
}

fun readHeaderField(buffer: BitBuffer): HeaderField? {
    if (buffer.readBit() == 1) {
        return readIndexedHeaderField(buffer)
    }
    if (buffer.readBit() == 1) {
        return readIndexedLiteralHeaderField(buffer)
    }
    if (buffer.readBit() == 1) {
        updateDynamicTableSize(buffer)
        return null
    }
    if (buffer.readBit() == 1) {
        return readNeverIndexedLiteralHeaderField(buffer)
    } else {
        return readNotIndexedLiteralHeaderField(buffer)
    }
}

fun readIndexedHeaderField(buffer: BitBuffer): HeaderField {
    val idx = buffer.readInt()
    return getHeaderField(idx)
}

fun readIndexedLiteralHeaderField(buffer: BitBuffer): HeaderField {
    val hf = readLiteralHeaderField(buffer)
    dynamicTable.add(hf)
    return hf
}

fun readNeverIndexedLiteralHeaderField(buffer: BitBuffer): HeaderField {
    return readLiteralHeaderField(buffer)
}

fun readNotIndexedLiteralHeaderField(buffer: BitBuffer): HeaderField {
    return readLiteralHeaderField(buffer)
}

fun readLiteralHeaderField(buffer: BitBuffer): HeaderField {
    val idx = buffer.readInt()
    val name: String
    if (idx == 0) {
        name = readString(buffer)
    } else {
        name = getHeaderField(idx).name
    }
    val value = readString(buffer)
    return HeaderField(name, value)
}

fun updateDynamicTableSize(buffer: BitBuffer) {
    val newSize = buffer.readInt()
    dynamicTable.setSize(newSize)
}

fun readInt(buffer: BitBuffer): Int {
    val prefix = buffer.bitsInOctet
    var value = buffer.readInt()
    if (value < ((1 shl prefix) - 1)) {
        return value
    }
    var m = 0
    do {
        var b = buffer.readByte()
        value = value + (b and 127) * (1 shl m)
        m = m + 7
    } while ((b and 128) == 128)
    return value
}

fun readString(buffer: BitBuffer): String {
    val huffman = buffer.readBit() == 1
    val len = buffer.readInt()
    val data = buffer.readBytes(len)
    if (huffman) {
        return decodeHuffman(data).toString(Charsets.UTF_8)
    } else {
        return data.toString(Charsets.UTF_8)
    }
}

fun decodeHuffman(data: ByteArray): ByteArray {
    // TODO implement
    return data
}

fun getHeaderField(idx: Int): HeaderField {
    if (idx < staticTable.size) {
        return staticTable.get(idx)
    }
    val dynIdx = idx - staticTable.size
    return dynamicTable.get(dynIdx)
}

data class HeaderField(val name: String, val value: String) {
    /** https://www.rfc-editor.org/rfc/rfc7541#section-4.1 */
    fun size(): Int {
        return name.length + value.length + 32
    }
}

val staticTable =
        listOf(
                HeaderField("PADDING", ""),
                HeaderField(":authority", ""),
                HeaderField(":method", "GET"),
                HeaderField(":method", "POST"),
                HeaderField(":path", "/"),
                HeaderField(":path", "/index.html"),
                HeaderField(":scheme", "http"),
                HeaderField(":scheme", "https"),
                HeaderField(":status", "200"),
                HeaderField(":status", "204"),
                HeaderField(":status", "206"),
                HeaderField(":status", "304"),
                HeaderField(":status", "400"),
                HeaderField(":status", "404"),
                HeaderField(":status", "500"),
                HeaderField("accept-charset", ""),
                HeaderField("accept-encoding", "gzip,deflate"),
                HeaderField("accept-language", ""),
                HeaderField("accept-ranges", ""),
                HeaderField("accept", ""),
                HeaderField("access-control-allow-origin", ""),
                HeaderField("age", ""),
                HeaderField("allow", ""),
                HeaderField("authorization", ""),
                HeaderField("cache-control", ""),
                HeaderField("content-disposition", ""),
                HeaderField("content-encoding", ""),
                HeaderField("content-language", ""),
                HeaderField("content-length", ""),
                HeaderField("content-location", ""),
                HeaderField("content-range", ""),
                HeaderField("content-type", ""),
                HeaderField("cookie", ""),
                HeaderField("date", ""),
                HeaderField("etag", ""),
                HeaderField("expect", ""),
                HeaderField("expires", ""),
                HeaderField("from", ""),
                HeaderField("host", ""),
                HeaderField("if-match", ""),
                HeaderField("if-modified-since", ""),
                HeaderField("if-none-match", ""),
                HeaderField("if-range", ""),
                HeaderField("if-unmodified-since", ""),
                HeaderField("last-modified", ""),
                HeaderField("link", ""),
                HeaderField("location", ""),
                HeaderField("max-forwards", ""),
                HeaderField("proxy-authenticate", ""),
                HeaderField("proxy-authorization", ""),
                HeaderField("range", ""),
                HeaderField("referer", ""),
                HeaderField("refresh", ""),
                HeaderField("retry-after", ""),
                HeaderField("server", ""),
                HeaderField("set-cookie", ""),
                HeaderField("strict-transport-security", ""),
                HeaderField("transfer-encoding", ""),
                HeaderField("user-agent", ""),
                HeaderField("vary", ""),
                HeaderField("via", ""),
                HeaderField("www-authenticate", "")
        )

val dynamicTable = DynamicHeaderFieldTable(4096)

class DynamicHeaderFieldTable(var maxSize: Int) {
    val entries: MutableList<HeaderField> = ArrayList<HeaderField>()
    var currentSize = 0

    /**
     * We evict entries until we can fit new entry. If new entry is larger than our maxSize, than we
     * will evict all entries and won't add this new entry - ending up with empty table
     */
    fun add(hf: HeaderField) {
        if (hf.size() + currentSize > maxSize) {
            evict(hf.size())
        }
        if (hf.size() + currentSize <= maxSize) {
            entries.add(0, hf)
        }
    }

    fun get(idx: Int): HeaderField {
        return entries.get(idx)
    }

    fun evict(additionalSize: Int) {
        while (currentSize + additionalSize > maxSize && entries.size > 0) {
            currentSize -= entries.last().size()
            entries.removeAt(entries.lastIndex)
        }
    }

    fun setSize(newSize: Int) {
        maxSize = newSize
        evict(0)
    }
}
