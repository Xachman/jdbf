package net.iryndin.jdbf.reader;

import net.iryndin.jdbf.core.MemoFileHeader;
import net.iryndin.jdbf.core.MemoRecord;
import net.iryndin.jdbf.util.BitUtils;
import net.iryndin.jdbf.util.IOUtils;
import net.iryndin.jdbf.util.JdbfUtils;

import java.io.*;

/**
 * Reader of memo files (tested of *.FPT files - Visual FoxPro)
 * See links: 
 * 
 * Visual FoxPro file formats:
 * http://msdn.microsoft.com/en-us/library/aa977077(v=vs.71).aspx
 * 
 * DBase file formats:
 * http://www.dbase.com/Knowledgebase/INT/db7_file_fmt.htm
 * 
 */
public class MemoReader implements Closeable {

    private static final int BUFFER_SIZE = 8192;
    private InputStream memoInputStream;
    private MemoFileHeader memoHeader;
    private File memoFile;

    public MemoReader(File memoFile) throws IOException {
        this(new FileInputStream(memoFile), memoFile);
    }

    public MemoReader(InputStream inputStream, File memoFile) throws IOException {
        this.memoInputStream = new BufferedInputStream(inputStream, BUFFER_SIZE);
        this.memoFile = memoFile;
        readMetadata();
        memoInputStream.close();
    }

    private void readMetadata() throws IOException {
        byte[] headerBytes = new byte[JdbfUtils.MEMO_HEADER_LENGTH];
        memoInputStream.mark(8192);
        memoInputStream.read(headerBytes);
        this.memoHeader = MemoFileHeader.create(headerBytes);
    }

    @Override
    public void close() throws IOException {
        if (memoInputStream != null) {
            memoInputStream.close();
        }
    }

    public MemoFileHeader getMemoHeader() {
        return memoHeader;
    }

    public MemoRecord read(int offsetInBlocks) throws IOException {
        InputStream memo = new FileInputStream(this.memoFile);
        memoInputStream = new BufferedInputStream(memo, BUFFER_SIZE);
        //System.out.println(this.memoHeader);
       // memoInputStream.mark(memoHeader.getBlockSize()*offsetInBlocks);
        memoInputStream.skip(memoHeader.getBlockSize()*offsetInBlocks);
        byte[] recordHeader = new byte[8];
        memoInputStream.read(recordHeader);
        int memoRecordLength = BitUtils.makeInt(recordHeader[7], recordHeader[6], recordHeader[5], recordHeader[4]);
        if(memoRecordLength < 0) {
            memoRecordLength = 0;
        }
        byte[] recordBody = new byte[memoRecordLength];
        memoInputStream.read(recordBody);
        memoInputStream.close();
        return new MemoRecord(recordHeader, recordBody, memoHeader.getBlockSize(), offsetInBlocks);
    }
}
