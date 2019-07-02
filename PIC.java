//package editor;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.filechooser.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public class TEXTURE // UNZLIBED TXS
{
	byte [] data;
	long length;
	/////////////////////////////////////////////////////
	// File structure
	/////////////////////////////////////////////////////
	long magic; //at 0x0 = 0 x 97 42 85 29
	long separator; // MUST BE EQUAL TO 0x10
	long resolutionSpecific1;
	long serialNumber;
	long something1; // TODO: FIND OUT WHAT THIS DOES
	int width;
	int height;
	int horizontalStretch;
	int verticalStretch;
	long something2; // TODO: FIND OUT WHAT THIS DOES
	long colorDepthMultiple; // colorDepth =  TODO: FIND FORMULA!
	long separator2; // MUST BE EQUAL TO 0x4
	long resolutionSpecific2;
	long resolutionSpecific3;
	
	palette_color [] palette;
	
	
	
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
		
	public TEXTURE(String filename) throws Exception
	{
		File file = new File(filename);
		this.length = (int) file.length();
		this.data = new byte[(int) length];
		DataInputStream dis = new DataInputStream(new FileInputStream(file));
		dis.readFully(data);
		dis.close();
		initialize();
		
	}

	public TEXTURE(byte [] data, long length)
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
		f  = new File(path, fileNames[index]+"-UNzlibed.TEXTURE");

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
		TEXTURE TEXTURE = new TEXTURE("unnamed_00000.TEXTURE");	
		String [] arr = TEXTURE.fileList();
		for (int i=0; i<arr.length; i++)
			System.out.println(arr[i]);

		TEXTURE.exportFile(0, "unnamed_00000");
	}
	
	class palette_color
	{
		int r;
		int g;
		int b;
		int a;
		palette_color()
		{
			r=0; g=0; b=0; a=0;
		}
	};
	
	
	
}
