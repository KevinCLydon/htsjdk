/**
 * ****************************************************************************
 * Copyright 2013 EMBL-EBI
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ****************************************************************************
 */
package htsjdk.samtools.cram.io;

import htsjdk.samtools.util.RuntimeIOException;

import java.io.IOException;
import java.io.OutputStream;

public class DefaultBitOutputStream extends OutputStream implements BitOutputStream {

    private static final byte[] bitMasks = new byte[8];

    static {
        for (byte i = 0; i < 8; i++)
            bitMasks[i] = (byte) (~(0xFF >>> i));
    }

    private final OutputStream out;

    private int bufferByte = 0;
    private int bufferedNumberOfBits = 0;

    public DefaultBitOutputStream(final OutputStream delegate) {
        this.out = delegate;
    }

    public void write(final byte b) {
        try {
            out.write((int) b);
        } catch (final IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    @Override
    public void write(final int value) {
        try {
            out.write(value);
        } catch (final IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    @Override
    public String toString() {
        return "DefaultBitOutputStream: "
                + Integer.toBinaryString(bufferByte).substring(0, bufferedNumberOfBits);
    }

    @Override
    public void write(final long bitContainer, final int nofBits) {
        if (nofBits == 0)
            return;

        if (nofBits < 1 || nofBits > 64)
            throw new RuntimeIOException("Expecting 1 to 64 bits, got: value=" + bitContainer + ", nofBits=" + nofBits);

        if (nofBits <= 8)
            write((byte) bitContainer, nofBits);
        else {
            for (int i = nofBits - 8; i >= 0; i -= 8) {
                final byte v = (byte) (bitContainer >>> i);
                writeByte(v);
            }
            if (nofBits % 8 != 0) {
                final byte v = (byte) bitContainer;
                write(v, nofBits % 8);
            }
        }
    }

    void write_int_LSB_0(final int value, final int nofBitsToWrite) {
        if (nofBitsToWrite == 0)
            return;

        if (nofBitsToWrite < 1 || nofBitsToWrite > 32)
            throw new RuntimeIOException("Expecting 1 to 32 bits.");

        if (nofBitsToWrite <= 8)
            write((byte) value, nofBitsToWrite);
        else {
            for (int i = nofBitsToWrite - 8; i >= 0; i -= 8) {
                final byte v = (byte) (value >>> i);
                writeByte(v);
            }
            if (nofBitsToWrite % 8 != 0) {
                final byte v = (byte) value;
                write(v, nofBitsToWrite % 8);
            }
        }
    }

    @Override
    public void write(final int bitContainer, final int nofBits) {
        write_int_LSB_0(bitContainer, nofBits);
    }

    private void writeByte(final int value) {
        try {
            if (bufferedNumberOfBits == 0)
                out.write(value);
            else {
                bufferByte = ((value & 0xFF) >>> bufferedNumberOfBits) | bufferByte;
                out.write(bufferByte);
                bufferByte = (value << (8 - bufferedNumberOfBits)) & 0xFF;
            }
        } catch (final IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    @Override
    public void write(byte bitContainer, final int nofBits) {
        if (nofBits < 0 || nofBits > 8)
            throw new RuntimeIOException("Expecting 0 to 8 bits.");

        if (nofBits == 8)
            writeByte(bitContainer);
        else {
            if (bufferedNumberOfBits == 0) {
                bufferByte = (bitContainer << (8 - nofBits)) & 0xFF;
                bufferedNumberOfBits = nofBits;
            } else {
                bitContainer = (byte) (bitContainer & ~bitMasks[8 - nofBits]);
                int bits = 8 - bufferedNumberOfBits - nofBits;
                try {
                    if (bits < 0) {
                        bits = -bits;
                        bufferByte |= (bitContainer >>> bits);
                        out.write(bufferByte);
                        bufferByte = (bitContainer << (8 - bits)) & 0xFF;
                        bufferedNumberOfBits = bits;
                    } else if (bits == 0) {
                        bufferByte = bufferByte | bitContainer;
                        out.write(bufferByte);
                        bufferedNumberOfBits = 0;
                    } else {
                        bufferByte = bufferByte | (bitContainer << bits);
                        bufferedNumberOfBits = 8 - bits;
                    }
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public void write(final boolean bit) {
        write(bit ? (byte) 1 : (byte) 0, 1);
    }

    @Override
    public void write(final boolean bit, final long repeat) {
        for (long i = 0; i < repeat; i++)
            write(bit);
    }

    @Override
    public void close() {
        flush();
        try {
            out.close();
        } catch (final IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    @Override
    public void flush() {
        try {
            if (bufferedNumberOfBits > 0)
                out.write(bufferByte);

            bufferedNumberOfBits = 0;
            out.flush();
        } catch (final IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    @Override
    public void write(@SuppressWarnings("NullableProblems") final byte[] b) {
        try {
            out.write(b);
        } catch (final IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    @Override
    public void write(@SuppressWarnings("NullableProblems") final byte[] b, final int off, final int length) {
        try {
            out.write(b, off, length);
        } catch (final IOException e) {
            throw new RuntimeIOException(e);
        }
    }

}
