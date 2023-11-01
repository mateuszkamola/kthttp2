package pl.mkamola.kthttp2.net.http2

fun getHeaderField(idx: Int) : HeaderField {
	if (idx < staticTable.size) {
		return staticTable.get(idx)
	}
	val dynIdx = idx - staticTable.size
	return dynamicTable.get(dynIdx)
}

data class HeaderField(val name: String, val value: String) {
	/**
	* https://www.rfc-editor.org/rfc/rfc7541#section-4.1
	*/
	fun size() : Int {
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

class DynamicHeaderFieldTable(val maxSize: Int) {
	val entries : MutableList<HeaderField> = ArrayList<HeaderField>()
	var currentSize = 0

	fun add(hf : HeaderField) {
		if (hf.size() + currentSize > maxSize) {
			evict(hf.size())
		}
		entries.add(0, hf)
	}

	fun get(idx : Int) : HeaderField {
		return entries.get(idx)
	}

	fun evict(additionalSize : Int) {
		while (currentSize + additionalSize > maxSize && entries.size > 0) {
			currentSize -= entries.last().size()
			entries.removeAt(entries.lastIndex)
		}
	}

}
