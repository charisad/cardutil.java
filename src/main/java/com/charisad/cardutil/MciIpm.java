package com.charisad.cardutil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

public class MciIpm {
    private static final Logger LOGGER = LoggerFactory.getLogger(MciIpm.class);

    // --- Streams for 1014 Blocking ---

    public static class Block1014OutputStream extends FilterOutputStream {
        private static final byte PAD_CHAR = 0x40;
        private int remainingChars = 1012;

        public Block1014OutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void write(int b) throws IOException {
            write(new byte[]{(byte) b});
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            int offset = off;
            int length = len;
            
            while (length > 0) {
                int chunk = Math.min(length, remainingChars);
                out.write(b, offset, chunk);
                offset += chunk;
                length -= chunk;
                remainingChars -= chunk;
                
                if (remainingChars == 0) {
                    out.write(PAD_CHAR);
                    out.write(PAD_CHAR);
                    remainingChars = 1012;
                }
            }
        }
        
        public void close() throws IOException {
             // Finalise
             for (int i = 0; i < remainingChars + 2; i++) {
                 out.write(PAD_CHAR);
             }
             super.close();
        }
    }

    public static class Unblock1014InputStream extends FilterInputStream {
        private byte[] buffer = new byte[0];
        private int bufferPos = 0;

        public Unblock1014InputStream(InputStream in) {
            super(in);
        }

        @Override
        public int read() throws IOException {
            byte[] b = new byte[1];
            int n = read(b, 0, 1);
            return n == -1 ? -1 : b[0] & 0xFF;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (len == 0) return 0;
            int totalRead = 0;
            
            while (totalRead < len) {
                if (bufferPos >= buffer.length) {
                    fillBuffer();
                    if (buffer.length == 0) { // EOF
                        return totalRead == 0 ? -1 : totalRead;
                    }
                }
                
                int available = buffer.length - bufferPos;
                int toCopy = Math.min(len - totalRead, available);
                System.arraycopy(buffer, bufferPos, b, off + totalRead, toCopy);
                bufferPos += toCopy;
                totalRead += toCopy;
            }
            return totalRead;
        }
        
        private void fillBuffer() throws IOException {
            byte[] block = new byte[1014];
            int n = readFully(in, block);
            if (n < 1014) {
                buffer = new byte[0]; // EOF or partial block (error?)
                return;
            }
            // Take only 1012
            buffer = new byte[1012];
            System.arraycopy(block, 0, buffer, 0, 1012);
            bufferPos = 0;
        }
        
        private int readFully(InputStream in, byte[] b) throws IOException {
            int n = 0;
            while (n < b.length) {
                int count = in.read(b, n, b.length - n);
                if (count < 0) break;
                n += count;
            }
            return n;
        }
    }

    // --- VBS Reader/Writer ---

    public static class VbsReader implements Closeable, Iterable<byte[]> {
        private final DataInputStream in;
        private byte[] nextRecord;

        public VbsReader(InputStream in, boolean blocked) {
            InputStream is = blocked ? new Unblock1014InputStream(in) : in;
            this.in = new DataInputStream(is);
        }

        @Override
        public Iterator<byte[]> iterator() {
            return new Iterator<byte[]>() {
                @Override
                public boolean hasNext() {
                    if (nextRecord != null) return true;
                    try {
                        nextRecord = readNext();
                        return nextRecord != null;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public byte[] next() {
                    if (!hasNext()) throw new NoSuchElementException();
                    byte[] r = nextRecord;
                    nextRecord = null;
                    return r;
                }
            };
        }

        public byte[] readNext() throws IOException {
            try {
                int len = in.readInt(); // reads 4 bytes big endian
                if (len == 0) return null; // End of VBS
                
                // Sanity check max length (from config, default 6000)
                if (len < 0 || len > 100000) { // arbitrary safe limit for sanity
                    throw new IOException("Invalid VBS record length: " + len);
                }
                
                byte[] data = new byte[len];
                in.readFully(data);
                
                // Return full record including 4 byte length header to match Python logic
                // Python: self.last_record = record_length_raw + record
                // return record (which includes header? No, python returns header + record)
                // "return self.last_record" ?? No.
                // Python VbsReader:
                // record = self.vbs_data.read(record_length)
                // return record (Waait. It returns only data if logic is normal)
                // Let's check python: "return record # get the full record including the record length"
                // Comment says "including record length".
                // Code: self.last_record = record_length_raw + record
                // return record
                // Wait, "return record" returns local var `record` which is just data.
                // BUT "return record # get the full record including the record length" comment is confusing.
                // Actually, IpmReader takes this record and calls loads(vbs_record).
                // loads(b) expects MTI + ID...
                // Iso8583 message does NOT usually include the VBS length header.
                // MTI (4) + Bitmap...
                // If I return header + data, then Iso8583.unpack will try to parse first 4 bytes as MTI.
                // If length is e.g. 00 00 00 50. MTI = "\0\0\0P". That's invalid MTI.
                // So I suspect VbsReader in Python returns ONLY the data body.
                // Let's check python `__next__`:
                // record = self.vbs_data.read(record_length)
                // ...
                // self.last_record = record_length_raw + record
                // return record
                // Yes, it returns only the data.
                // The comment "including the record length" in python docstring might be wrong or referring to `last_record`.
                // I will return ONLY data.
                
                return data; 
            } catch (EOFException e) {
                return null;
            }
        }

        @Override
        public void close() throws IOException {
            in.close();
        }
    }

    public static class VbsWriter implements Closeable {
        private final DataOutputStream out;

        public VbsWriter(OutputStream out, boolean blocked) {
            OutputStream os = blocked ? new Block1014OutputStream(out) : out;
            this.out = new DataOutputStream(os);
        }

        public void write(byte[] record) throws IOException {
            out.writeInt(record.length);
            out.write(record);
        }

        @Override
        public void close() throws IOException {
            out.writeInt(0); // Zero length record terminator
            out.close();
        }
    }
    
    // --- IPM Reader/Writer ---

    public static class IpmReader implements Closeable, Iterable<Map<String, Object>> {
        private final VbsReader vbsReader;
        private final Charset encoding;
        private final Map<Integer, BitConfig> config;

        public IpmReader(InputStream in, boolean blocked) {
            this(in, blocked, StandardCharsets.ISO_8859_1, null);
        }

        public IpmReader(InputStream in, boolean blocked, Charset encoding, Map<Integer, BitConfig> config) {
            this.vbsReader = new VbsReader(in, blocked);
            this.encoding = encoding;
            this.config = config;
        }

        @Override
        public Iterator<Map<String, Object>> iterator() {
            final Iterator<byte[]> vbsIter = vbsReader.iterator();
            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return vbsIter.hasNext();
                }

                @Override
                public Map<String, Object> next() {
                    byte[] data = vbsIter.next();
                    return Iso8583.unpack(data, config, encoding, false);
                }
            };
        }

        @Override
        public void close() throws IOException {
            vbsReader.close();
        }
    }

    public static class IpmWriter implements Closeable {
        private final VbsWriter vbsWriter;
        private final Charset encoding;
        private final Map<Integer, BitConfig> config;
        
        public IpmWriter(OutputStream out, boolean blocked) {
             this(out, blocked, StandardCharsets.ISO_8859_1, null);
        }

        public IpmWriter(OutputStream out, boolean blocked, Charset encoding, Map<Integer, BitConfig> config) {
            this.vbsWriter = new VbsWriter(out, blocked);
            this.encoding = encoding;
            this.config = config;
        }
        
        public void write(Map<String, Object> message) throws IOException {
            byte[] data = Iso8583.pack(message, config, encoding, false);
            vbsWriter.write(data);
        }
        
        @Override
        public void close() throws IOException {
            vbsWriter.close();
        }
    }
}
