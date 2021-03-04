package com.meowlomo.ci.ems.bundle.file;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipCompressUtil {
	static final int BUFFER = 8192;

	public static void compress(String srcPath , String destPath) throws IOException{
		compress(srcPath, destPath, true);
	}
	
	/**
	 * 
	 * @param srcPath 被压缩的目录/文件
	 * @param destPath 想要生成的压缩文件(如果只是名字则与srcPath生成在同一目录）
	 * @param needBaseDir
	 * @return 生成的压缩文件绝对路径
	 * @throws IOException
	 */
	public static String compress(String srcPath , String destPath, boolean needBaseDir) throws IOException{
	    File srcFile = new File(srcPath);
	    if (!destPath.toLowerCase().endsWith(".zip")) {
	    	destPath += ".zip";
	    }
	    if (!destPath.contains(File.separator)) {
	    	destPath = srcFile.getParent() + File.separator + destPath;
	    }
	    File destFil = new File(destPath);
	    
	    if (!srcFile.exists()) {
	        throw new FileNotFoundException(srcPath + "not exist ！");
	    }

	    FileOutputStream out = null;
	    ZipOutputStream zipOut = null;
	    CheckedOutputStream cos = null;
	    try {
	        out = new FileOutputStream(destFil);
	        cos = new CheckedOutputStream(out,new CRC32());
	        zipOut = new ZipOutputStream(cos);
	        String baseDir = "";
	        compress(srcFile, zipOut, baseDir, needBaseDir);
	    }
	    finally {
	        if(null != zipOut){
	            zipOut.close();
	        } else if (null != cos) {
	        	cos.close();
	        } else if(null != out){
	            out.close();
	        }
	        out = null;
            cos = null;
            zipOut = null;
            return destPath;
	    }
	}

	private static void compress(File file, ZipOutputStream zipOut, String baseDir, boolean needBaseDir) throws IOException{
	    if (file.isDirectory()) {
	        compressDirectory(file, zipOut, baseDir, needBaseDir);
	    } else {
	        compressFile(file, zipOut, baseDir, needBaseDir);
	    }
	}

	/**
	 * 压缩目录
	 * @param dir
	 * @param zipOut
	 * @param baseDir
	 * @throws IOException
	 */
	private static void compressDirectory(File dir, ZipOutputStream zipOut, String baseDir, boolean needBaseDir) throws IOException{
	    File[] files = dir.listFiles();
	    for (int i = 0; i < files.length; i++) {
	    	String compressPath = baseDir + dir.getName();
			if (!needBaseDir) {
				int index = compressPath.indexOf(File.separatorChar);
				if (index >= 0) {
					compressPath = compressPath.substring(index);
				} else {
					compressPath = "";
				}
			}
			
			if (!compressPath.isEmpty())
				compressPath += "/";
			compress(files[i], zipOut, compressPath, true);
	    }
	}

	/**
	 * 压缩单独文件
	 * @param file
	 * @param zipOut
	 * @param baseDir
	 * @throws IOException
	 */
	private static void compressFile(File file, ZipOutputStream zipOut, String baseDir, boolean needBaseDir)  throws IOException{
	    if (!file.exists()){
	        return;
	    }

	    BufferedInputStream bis = null;
	    try {
	        bis = new BufferedInputStream(new FileInputStream(file));
	        ZipEntry entry = new ZipEntry(baseDir + file.getName());
	        zipOut.putNextEntry(entry);
	        int count;
	        byte data[] = new byte[BUFFER];
	        while ((count = bis.read(data, 0, BUFFER)) != -1) {
	            zipOut.write(data, 0, count);
	        }

	    }finally {
	        if(null != bis){
	            bis.close();
	        }
	    }
	}
	
	/**
	 * 解压缩
	 * @param zipFile
	 * @param dstPath
	 * @throws IOException
	 */
	public static void decompress(String zipFile , String dstPath)throws IOException{
	    File pathFile = new File(dstPath);
	    if(!pathFile.exists()){
	        pathFile.mkdirs();
	    }
	    ZipFile zip = new ZipFile(zipFile);
	    for(Enumeration entries = zip.entries();entries.hasMoreElements();){
	        ZipEntry entry = (ZipEntry)entries.nextElement();
	        String zipEntryName = entry.getName();
	        InputStream in = null;
	        OutputStream out = null;
	        try{
	            in =  zip.getInputStream(entry);
	            String outPath = (dstPath+"/"+zipEntryName).replaceAll("\\*", "/");;
	            //判断路径是否存在,不存在则创建文件路径
	            File file = new File(outPath.substring(0, outPath.lastIndexOf('/')));
	            if(!file.exists()){
	                file.mkdirs();
	            }
	            //判断文件全路径是否为文件夹,如果是上面已经上传,不需要解压
	            if(new File(outPath).isDirectory()){
	                continue;
	            }

	            out = new FileOutputStream(outPath);
	            byte[] buf1 = new byte[1024];
	            int len;
	            while((len=in.read(buf1))>0){
	                out.write(buf1,0,len);
	            }
	        }
	        finally {
	            if(null != in){
	                in.close();
	            }

	            if(null != out){
	                out.close();
	            }
	        }
	    }
	}
}

