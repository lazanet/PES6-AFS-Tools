//package editor;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.filechooser.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public class TXS
{
	byte [] data;
	long length;
	/////////////////////////////////////////////////////
	// File structure
	/////////////////////////////////////////////////////
	long magic; //at 0x0
	long fileLength; // Counted from 0x20 (so all other values must be 0x20 padded)
	//padding (0x1a bytes)
	long numOfFiles; //at 0x20
	long separator; // MUST BE EQUAL TO 0x10
	long filePosList; // filePos[i] = 0x20 + (4*i) + filePosList;
	//padding (0x01 bytes)
	long [] namesPos; // namesPos[numOfFiles]
	//padding (some bytes)
	long [] fileStart;// fileHeader[i] = 0x20 + fileStart[i];
	////////////////////////////////////////////////////
	
	String [] fileNames;
	long offset;

	long tmpSubHeader;
	long tmpSubLength;
	long tmpSubFooter;
	byte [] tmpSubData;

	public static long readUInt32(RandomAccessFile dis) // long because int can't be unsigned...
	{
		try
		{
			long tmp = Integer.reverseBytes(dis.readInt()) & 0xFFFFFFFFL; // fix byte order and negative values
			return tmp;
		}
		catch(Exception e)
		{
			return -1;
		}
	}
	public long readUInt32()
	{
		int tmp = 0x0;
		tmp = data[(int) offset++];
		tmp = (tmp<<8) + data[(int) offset++];
		tmp = (tmp<<8) + data[(int) offset++];
		tmp = (tmp<<8) + data[(int) offset++];
		long t = Integer.reverseBytes(tmp) & 0xFFFFFFFFL;
		return t;
	}
		
	public TXS(String filename) throws Exception
	{
		File file = new File(filename);
		this.length = (int) file.length();
		this.data = new byte[(int) length];
		DataInputStream dis = new DataInputStream(new FileInputStream(file));
		dis.readFully(data);
		dis.close();
		initialize();
		
	}

	public TXS(byte [] data, long length)
	{
		this.length = length;
		this.data = data;
		initialize();
	}

	public void initialize()
	{
		offset = 0;
		
		magic = readUInt32();
		fileLength = readUInt32();
		
		offset = 0x20;
		
		numOfFiles = readUInt32();
		separator = readUInt32();
		filePosList = readUInt32();
	
		offset += 0x4;

		namesPos = new long[(int) numOfFiles];
		for (int i=0; i<numOfFiles; i++)
			namesPos[i] = readUInt32();

		fileNames = new String[(int) numOfFiles];		
		for (int i=0; i<numOfFiles; i++)
		{
			offset = 0x20 + namesPos[i];
			fileNames[i] = "";
//			System.out.println(Long.toHexString(offset));
			for (int j=0; j<16 && (data[(int)offset]!=0x0); j++)
				fileNames[i] += (char)data[(int) offset++];
		}

		offset = 0x20 + filePosList;
		fileStart = new long[(int) numOfFiles];
		for (int i=0; i<numOfFiles; i++)
			fileStart[i] = readUInt32();			
	}

	public String[] fileList()
	{
		String[] arr = new String[(int) numOfFiles];
		for (int i=0; i<numOfFiles; i++)
			arr[i] = fileNames[i];
		return arr;
	}

	public byte[] readFile(int index) throws Exception
	{
		offset = 0x20 + fileStart[index];
		tmpSubHeader = readUInt32();
		tmpSubLength = readUInt32();
		tmpSubFooter = readUInt32();
		offset = 0x20 + fileStart[index];
		byte [] file = Arrays.copyOfRange(data, (int) (0x20 + offset), (int) (0x20 + offset + tmpSubLength));
		
		// UNZLIB!

		Inflater decompresser = new Inflater();
		decompresser.setInput(file);
		ByteArrayOutputStream bos = new ByteArrayOutputStream(file.length);
		byte[] buffer = new byte[8192];
		while (!decompresser.finished())
		{
			int size = decompresser.inflate(buffer);
			bos.write(buffer, 0, size);
		}
		tmpSubData = bos.toByteArray();
		decompresser.end();
		return tmpSubData;
		//return file;
	}

	public void exportFile(int index, String path) throws Exception
	{
		File f  = new File(path);
		f.mkdirs();
		f  = new File(path, fileNames[index]+"-UNzlibed.txs");

		try (FileOutputStream fos = new FileOutputStream(f)) 
		{
   			fos.write(readFile(index));
   			fos.close();
		}
		catch(Exception e)
		{
			throw e;
		}	
	}
	public static void main(String[] args) throws Exception
	{
		TXS txs = new TXS("unnamed_00000.txs");	
		String [] arr = txs.fileList();
		for (int i=0; i<arr.length; i++)
			System.out.println(arr[i]);

		txs.exportFile(0, "unnamed_00000");
	}

	
	
	
}
