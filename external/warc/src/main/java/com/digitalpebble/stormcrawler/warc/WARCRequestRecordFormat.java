package com.digitalpebble.stormcrawler.warc;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.storm.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.digitalpebble.stormcrawler.Metadata;

/**
 * Generate a byte representation of a WARC request record from a tuple if the
 * request HTTP headers are present. The request record ID is stored in the
 * metadata so that a WARC response record (created later) can refer to it.
 **/
@SuppressWarnings("serial")
public class WARCRequestRecordFormat extends WARCRecordFormat {

    private static final Logger LOG = LoggerFactory
            .getLogger(WARCRequestRecordFormat.class);

    @Override
    public byte[] format(Tuple tuple) {

        String url = tuple.getStringByField("url");
        Metadata metadata = (Metadata) tuple.getValueByField("metadata");

        String headersVerbatim = metadata.getFirstValue("_request.headers_");
        byte[] httpheaders = new byte[0];
        if (StringUtils.isBlank(headersVerbatim)) {
            // no request header: return empty record
            LOG.warn("No request header for {}", url);
            return new byte[] {};
        } else {
            // check that ends with an empty line
            if (!headersVerbatim.endsWith(CRLF + CRLF)) {
                headersVerbatim += CRLF + CRLF;
            }
            httpheaders = headersVerbatim.getBytes();
        }

        StringBuilder buffer = new StringBuilder();
        buffer.append(WARC_VERSION);
        buffer.append(CRLF);
        buffer.append("WARC-Type").append(": ").append("request").append(CRLF);

        String mainID = UUID.randomUUID().toString();
        buffer.append("WARC-Record-ID").append(": ").append("<urn:uuid:")
                .append(mainID).append(">").append(CRLF);
        /*
         * The request record ID is stored in the metadata so that a WARC
         * response record can later refer to it.
         */
        metadata.addValue("_request.warc_record_id_", mainID);

        int contentLength = httpheaders.length;
        buffer.append("Content-Length").append(": ")
                .append(Integer.toString(contentLength)).append(CRLF);

        String blockDigest = getDigestSha1(httpheaders);

        // get actual fetch time from metadata if any
        String captureTime = metadata.getFirstValue("_request.time_");
        if (captureTime == null) {
            Date now = new Date();
            captureTime = WARC_DF.format(now);
        }
        buffer.append("WARC-Date").append(": ").append(captureTime)
                .append(CRLF);

        // must be a valid URI
        try {
            String normalised = url.replaceAll(" ", "%20");
            String targetURI = URI.create(normalised).toASCIIString();
            buffer.append("WARC-Target-URI").append(": ").append(targetURI)
                    .append(CRLF);
        } catch (Exception e) {
            LOG.warn("Incorrect URI: {}", url);
            return new byte[] {};
        }

        buffer.append("Content-Type: application/http; msgtype=request")
                .append(CRLF);
        buffer.append("WARC-Block-Digest").append(": ").append(blockDigest)
                .append(CRLF);

        byte[] buffasbytes = buffer.toString().getBytes(StandardCharsets.UTF_8);

        int capacity = 6 + buffasbytes.length + httpheaders.length;

        ByteBuffer bytebuffer = ByteBuffer.allocate(capacity);
        bytebuffer.put(buffasbytes);
        bytebuffer.put(CRLF_BYTES);
        bytebuffer.put(httpheaders);
        bytebuffer.put(CRLF_BYTES);
        bytebuffer.put(CRLF_BYTES);

        return bytebuffer.array();
    }

}
