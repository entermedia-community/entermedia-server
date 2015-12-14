package org.entermediadb.asset.xmp;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

public class IndesignFile
{
	protected byte[] validGUID = new byte[] {
			0x06, 0x06, (byte)0xed, (byte)0xf5,
			(byte)0xd8, 0x1d, 0x46, (byte)0xe5,
			(byte)0xbd, 0x31, (byte)0xef, (byte)0xe7,
			(byte)0xfe, 0x74, (byte)0xb7, 0x1d
	};
	protected MasterPage[] fieldMasterPages;
	protected boolean fieldValid = false;
	
	public IndesignFile(File inFile)
	{
		fieldMasterPages = new MasterPage[2];
		try
		{
			DataInputStream in = new DataInputStream(new FileInputStream(inFile));
			fieldMasterPages[0] = readMasterPage(in);
			fieldMasterPages[1] = readMasterPage(in);
			in.close();
			setValid(Arrays.equals(fieldMasterPages[0].getGUID(), validGUID));
		}
		catch(Exception e)
		{
			setValid(false);
		}
	}
	
	protected MasterPage readMasterPage(DataInputStream inIn) throws Exception
	{
		MasterPage mp = new MasterPage();
		byte[] buf;
		
		buf = new byte[16];
		inIn.read(buf, 0, buf.length);
		mp.setGUID(buf);
		
		buf = new byte[8];
		inIn.read(buf, 0, buf.length);
		mp.setMagicBytes(new String(buf));
		
		mp.setObjectStreamEndian(inIn.read());
		
		inIn.skip(239);
		
		mp.setSequenceNumber(inIn.readLong());
		
		inIn.skip(8);
		
		mp.setFilePages(inIn.readInt());
		
		inIn.skip(3812);
		
		return mp;
	}
	
	public boolean isValid()
	{
		return fieldValid;
	}

	protected void setValid(boolean valid)
	{
		fieldValid = valid;
	}

	protected static int byteArrayToInt(byte[] b) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (b[i] & 0x000000FF) << shift;
        }
        return value;
    }
	
	public MasterPage getActualMasterPage()
	{
		if (!isValid())
		{
			return null;
		}
		
		if (fieldMasterPages[0].getSequenceNumber() > fieldMasterPages[1].getSequenceNumber())
		{
			return fieldMasterPages[0];
		}
		return fieldMasterPages[1];
	}
	
}

class MasterPage
{
	protected byte[] fieldGUID;
	protected String fieldMagicBytes;
	protected int fieldObjectStreamEndian;
	protected long fieldSequenceNumber;
	protected int fieldFilePages;
	
	public byte[] getGUID()
	{
		return fieldGUID;
	}
	public void setGUID(byte[] guid)
	{
		fieldGUID = guid;
	}
	public String getMagicBytes()
	{
		return fieldMagicBytes;
	}
	public void setMagicBytes(String magicBytes)
	{
		fieldMagicBytes = magicBytes;
	}
	public int getObjectStreamEndian()
	{
		return fieldObjectStreamEndian;
	}
	public void setObjectStreamEndian(int objectStreamEndian)
	{
		fieldObjectStreamEndian = objectStreamEndian;
	}
	public long getSequenceNumber()
	{
		return fieldSequenceNumber;
	}
	public void setSequenceNumber(long sequenceNumber)
	{
		fieldSequenceNumber = sequenceNumber;
	}
	public int getFilePages()
	{
		return fieldFilePages;
	}
	public void setFilePages(int filePages)
	{
		fieldFilePages = filePages;
	}
	
	
}