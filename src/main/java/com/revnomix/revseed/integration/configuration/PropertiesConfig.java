package com.revnomix.revseed.integration.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class PropertiesConfig {

	public static final String COMPLETED_MGS_CHANNEL = "completedChannel";

	@Value("${local.directory.rateshop:c:/revseed/incomingDirectory/rateshop}")
	private String incomingDirectoryPath;

	@Value("${local.directory.rateshop:c:/revseed/incomingDirectory/hnf}")
	private String hnfIncomingDirectoryPath;

	@Value("${local.directory.staah:c:/revseed/incomingDirectory/staah}")
	private String staahIncomingDirectoryPath;

	@Value("${local.directory:c:/revseed/archiveDirectory}")
	private String archiveDirectoryPath;

	@Value("${local.directory:c:/revseed/iSellReportDirectory}")
	private String iSellReportDirectoryPath;

	@Value("${local.directory.error:c:/revseed/errorDirectory}")
	private String errorDirectoryPath;

	@Value("${local.directory.validate:c:/revseed/validateDirectory}")
	private String csvValidationDirectoryPath;

	@Value("${local.directory.validatehnf:c:/revseed/incomingDirectory/hnf}")
	private String hnfValidationDirectoryPath;

	@Value("${local.directory.validatehnf:c:/revseed/hnFvalidateDirectory}")
	private String hnfValidationDirectoryPathforexcel;

	@Value("${scheduler.thread.pool.size:10}")
	private Integer threadPoolExecutorPoolSize;

	@Value("${ftp.ratemetrics.ftp.port:2121}")
	private Integer ftpPort;

	@Value("${ftp.ratemetrics.ftp.host:localhost}")
	private String ftpHost;

	@Value("${ftp.ratemetrics.ftp.user:guest}")
	private String ftpUserName;

	@Value("${ftp.ratemetrics.ftp.password:guest}")
	private String ftpPassword;

	public Integer getThreadPoolExecutorPoolSize() {
		return threadPoolExecutorPoolSize;
	}

	public void setThreadPoolExecutorPoolSize(Integer threadPoolExecutorPoolSize) {
		this.threadPoolExecutorPoolSize = threadPoolExecutorPoolSize;
	}

	public String getStaahIncomingDirectoryPath() {
		return staahIncomingDirectoryPath;
	}

	public void setStaahIncomingDirectoryPath(String staahIncomingDirectoryPath) {
		this.staahIncomingDirectoryPath = staahIncomingDirectoryPath;
	}

	public void setArchiveDirectoryPath(String archiveDirectoryPath) {
		this.archiveDirectoryPath = archiveDirectoryPath;
	}

	public Integer getFtpPort() {
		return ftpPort;
	}

	public void setFtpPort(Integer ftpPort) {
		this.ftpPort = ftpPort;
	}

	public String getFtpHost() {
		return ftpHost;
	}

	public void setFtpHost(String ftpHost) {
		this.ftpHost = ftpHost;
	}

	public String getFtpUserName() {
		return ftpUserName;
	}

	public void setFtpUserName(String ftpUserName) {
		this.ftpUserName = ftpUserName;
	}

	public String getFtpPassword() {
		return ftpPassword;
	}

	public void setFtpPassword(String ftpPassword) {
		this.ftpPassword = ftpPassword;
	}

	public void setIncomingDirectoryPath(String incomingDirectoryPath) {
		this.incomingDirectoryPath = incomingDirectoryPath;
	}

	public String getIncomingDirectoryPath() {
		return incomingDirectoryPath;
	}

	public File getIncomingDirectory() {
		return new File(incomingDirectoryPath);
	}

	public File getHnFIncomingDirectory() {
		return new File(hnfIncomingDirectoryPath);
	}

	public File getArchiveDirectory() {
		return new File(archiveDirectoryPath);
	}

	public File getErrorDirectory() {
		return new File(errorDirectoryPath);
	}

	public File getcsvValidationDirectory() {
		return new File(csvValidationDirectoryPath);
	}

	public String getArchiveDirectoryPath() {
		return archiveDirectoryPath;
	}

	public String getErrorDirectoryPath() {
		return errorDirectoryPath;
	}

	public String getcsvValidationDirectoryPath() {
		return csvValidationDirectoryPath;
	}

	public void setErrorDirectoryPath(String errorDirectoryPath) {
		this.errorDirectoryPath = errorDirectoryPath;
	}

	public void setcsvValidationDirectoryPath(String csvValidationDirectoryPath) {
		this.csvValidationDirectoryPath = csvValidationDirectoryPath;
	}

	public String getiSellReportDirectoryPath() {
		return iSellReportDirectoryPath;
	}

	public void setiSellReportDirectoryPath(String iSellReportDirectoryPath) {
		this.iSellReportDirectoryPath = iSellReportDirectoryPath;
	}

	public String getHnfValidationDirectoryPath() {
		return hnfValidationDirectoryPath;
	}

	public void setHnfValidationDirectoryPath(String hnfValidationDirectoryPath) {
		this.hnfValidationDirectoryPath = hnfValidationDirectoryPath;
	}

	public String getHnfValidationDirectoryPathforexcel() {
		return hnfValidationDirectoryPathforexcel;
	}

	public void setHnfValidationDirectoryPathforexcel(String hnfValidationDirectoryPathforexcel) {
		this.hnfValidationDirectoryPathforexcel = hnfValidationDirectoryPathforexcel;
	}

	public String getHnfIncomingDirectoryPath() {
		return hnfIncomingDirectoryPath;
	}

	public void setHnfIncomingDirectoryPath(String hnfIncomingDirectoryPath) {
		this.hnfIncomingDirectoryPath = hnfIncomingDirectoryPath;
	}

}
