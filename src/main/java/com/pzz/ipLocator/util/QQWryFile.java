package com.pzz.ipLocator.util;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

import java.io.*;

public class QQWryFile {
	private final static String IP_FILE = getQQWryFilePath();
	private static final int IP_RECORD_LENGTH = 7;
	private RandomAccessFile ipFile = null;

	public RandomAccessFile getIpFile() {
		return ipFile;
	}

	private static QQWryFile instance = null;

	public static String getQQWryFilePath() {
		try {
			Resource resource = new ClassPathResource("qqwry.dat");
			InputStream inputStream = resource.getInputStream();
			File f = new File("qqwry-tmp.dat");
			FileOutputStream o = new FileOutputStream(f);
			FileCopyUtils.copy(inputStream,o);
			inputStream.close();
			o.close();
			return f.getPath();
		} catch (Exception e) {
			System.out.println("没有找到qqwry.dat文件");
			e.printStackTrace();
			return null;
		}
	}

	public QQWryFile() {
		try {
			if (null == IP_FILE)
				System.exit(1);
			ipFile = new RandomAccessFile(IP_FILE, "r");
		} catch (IOException e) {
			System.out.println("无法打开" + IP_FILE + "文件");
		}
	}

	public void closeIpFile(RandomAccessFile ipFile) {
		try {
			ipFile.close();
			ipFile = null;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (null != ipFile)
				ipFile = null;
		}
	}

	public synchronized static QQWryFile getInstance() {
		if (null == instance)
			instance = new QQWryFile();
		return instance;
	}

	public QQWryRecord find(String ip, RandomAccessFile ipFile) {
		long ipValue = Utils.ipToLong(ip);
		QQWryHeader header = new QQWryHeader(ipFile);
		long first = header.getIpBegin();
		int left = 0;
		int right = (int) ((header.getIpEnd() - first) / IP_RECORD_LENGTH);
		int middle = 0;
		QQWryIndex middleIndex = null;
		// 二分查找
		while (left <= right) {
			// 无符号右移，防止溢出
			middle = (left + right) >>> 1;
			middleIndex = new QQWryIndex(ipFile, first + middle
					* IP_RECORD_LENGTH);
			if (ipValue > middleIndex.getStartIp())
				left = middle + 1;
			else if (ipValue < middleIndex.getStartIp())
				right = middle - 1;
			else
				return new QQWryRecord(ipFile, middleIndex.getStartIp(),
						middleIndex.getIpPos());
		}
		// 找不到精确的，取在范围内的
		middleIndex = new QQWryIndex(ipFile, first + right * IP_RECORD_LENGTH);
		QQWryRecord record = new QQWryRecord(ipFile, middleIndex.getStartIp(),
				middleIndex.getIpPos());
		if (ipValue >= record.getBeginIP() && ipValue <= record.getEndIP()) {
			return record;
		} else {
			// 找不到相应的记录
			return new QQWryRecord(0L, ipValue);
		}
	}


	public static void main(String[] args)  {
		String ip = "202.108.22.5";
		// ip = "202.108.9.155";

		QQWryFile qqWryFile = QQWryFile.getInstance();
		RandomAccessFile ipFile = qqWryFile.getIpFile();
		QQWryRecord record = qqWryFile.find(ip, ipFile);

		System.out.println(Utils.ipToStr(record.getBeginIP()));
		System.out.println(Utils.ipToStr(record.getEndIP()));
		System.out.println(record.getCountry());
		System.out.println(record.getArea());

		// System.out.println(Utils.ipToStr(16842751));
		// System.out.println(Utils.ipToLong(ip));
		// qqWryFile.storageToPg(ipFile, 100);
		// qqWryFile.storageToMongoDB(ipFile, 100);
		
		qqWryFile.closeIpFile(ipFile);
		qqWryFile = null;
	}
}
