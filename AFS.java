//package editor;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.filechooser.*;
import java.io.*;

public class AFS
{
	/////////////////////////////////////////////////////
	// File structure
	/////////////////////////////////////////////////////
	String afsLocation;
	String magic; // 3

	long numOfFiles;
	ItemInfo[] itemInfo;
	NameInfo[] nameInfo;
	////////////////////////////////////////////////////
	
	String [] filename;	
	
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
	
	public AFS(String filename) throws Exception
	{
		afsLocation = filename;
		RandomAccessFile dis;
		try
		{
			dis = new RandomAccessFile(filename, "r");

			magic = "";
			for (int i = 0; i<3; i++)
				magic += (char) dis.readByte();

			dis.readByte(); // string terminator

			if (!magic.equals("AFS"))
				throw new Exception("File is not a valid AFS! Magic string is:"+magic);
			
			numOfFiles = readUInt32(dis);
			System.out.println("Number of files: " + numOfFiles); 
			if (numOfFiles==0)
				throw new Exception("AFS is empty!");
			
			itemInfo = new ItemInfo[(int) numOfFiles];
			nameInfo = new NameInfo[(int) numOfFiles];		
			
			for (int i=0; i<numOfFiles; i++)
			{
				itemInfo[i] = new ItemInfo();
				itemInfo[i].offset = readUInt32(dis);
				itemInfo[i].size   = readUInt32(dis);
				//System.out.println("i: "+i+" offset:"+Long.toHexString(itemInfo[i].offset)+" size:"+Long.toHexString(itemInfo[i].size));
			}

			long addrNameInfo = 0;
			while (addrNameInfo == 0)
			{
				addrNameInfo = readUInt32(dis);
				dis.readInt();
			}
			dis.seek(addrNameInfo);

			for (int i=0; i<numOfFiles; i++)
			{
				nameInfo[i] = new NameInfo();
				nameInfo[i].legacyfilename = "";
				for (int j=0; j<32; j++)
					nameInfo[i].legacyfilename += (char)dis.readByte(); 
				// Since KONAMI uses these entries for something else in WE/PES games (undocumented), we ignore them 
				// and append "unnamed_" or name from custom made AFL files
				filename[i] = "unnamed_" + String.format("%05d", i); // TODO: AFL PARSING!
				
				long oldPos = dis.getFilePointer();
				dis.seek(itemInfo[i].offset);
				long fileDescr = dis.readInt();

				if(fileDescr == 0x00060100)
					filename[i]+=".str";
				else if(fileDescr == 0x00020000) 		
					filename[i]+=".txs";
				else if(fileDescr == 0x000E0100)
					filename[i]+=".opd";
				else if(fileDescr == 0x00010100)
					filename[i]+=".fnt";
				else if (fileDescr == 0x00050000)
					filename[i]+=".flg";
				else if ((fileDescr & 0xFFFF0000) == 0x80000000)
					filename[i]+=".adx";
				else
					filename[i]+=".bin";		

				dis.seek(oldPos);
	
				for (int j=0; j<16; j++)
					nameInfo[i].other[j] = dis.readByte();
			}

			dis.close();
		}
		catch (Exception e)
		{
			throw e;
		}
	}
	
	public String[] fileList()
	{
		String[] arr = new String[(int)numOfFiles];
		for (int i=0; i<numOfFiles; i++)
			arr[i] = filename[i];
		return arr;
	}
	
	public byte[] readFile(int index) throws Exception
	{
		byte[] file = new byte[(int)itemInfo[index].size];
		RandomAccessFile dis = new RandomAccessFile(afsLocation, "r");
		//System.out.println(Long.toHexString(itemInfo[index].offset) + " " +Long.toHexString(itemInfo[index].size));
		dis.seek((int)itemInfo[index].offset);
		dis.read(file, 0, (int)itemInfo[index].size);
		dis.close();
		return file;
	}

	public void exportFile(int index, String path) throws Exception
	{
		File f  = new File(path);
		f.mkdirs();
		f = new File(path, nameInfo[index].filename);
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
		AFS afs = new AFS("e_text.afs");	
		String[] list = afs.fileList();
		for (int i=0; i<list.length; i++)
			afs.exportFile(i,"dat");
			//System.out.println(list[i]);
		afs.exportFile(0,".");
	}




	class ItemInfo
	{
		public long offset;
		public long size;
		public ItemInfo()
		{
			offset = 0;
			size = 0;
		}
	};
	
	class NameInfo
	{
		public String legacyfilename; //32 bytes
		public byte [] other;   // 16 bytes
		
		public NameInfo()
		{
			other = new byte[16];
		}
	};
	
	
}
