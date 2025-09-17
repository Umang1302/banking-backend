package com.nedbank.banking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import javax.sql.DataSource;
import java.sql.Connection;

@SpringBootApplication
public class BankingApplication {

	private static final Logger logger = LoggerFactory.getLogger(BankingApplication.class);

	@Autowired
	private DataSource dataSource;

	public static void main(String[] args) {
		logger.info("🏦 Starting Nedbank Banking Application...");
		
		try {
			SpringApplication.run(BankingApplication.class, args);
			logger.info("🚀 Nedbank Banking Application started successfully!");
		} catch (Exception e) {
			logger.error("💥 Failed to start Nedbank Banking Application", e);
			System.exit(1);
		}
	}

	@EventListener(ApplicationReadyEvent.class)
	public void logDatabaseConnectivityOnStartup() {
		logger.info("=== DATABASE CONNECTIVITY CHECK AT STARTUP ===");
		
		try {
			long startTime = System.currentTimeMillis();
			
			try (Connection connection = dataSource.getConnection()) {
				if (connection != null && connection.isValid(5)) { // 5 seconds timeout
					long responseTime = System.currentTimeMillis() - startTime;
					
					logger.info("✅ DATABASE CONNECTION: SUCCESSFUL");
					logger.info("   Connection Response Time: {}ms", responseTime);
					logger.info("   Database URL: {}", connection.getMetaData().getURL());
					logger.info("   Driver: {}", connection.getMetaData().getDriverName());
					logger.info("📊 Database is ready to accept connections");
					
				} else {
					logger.error("❌ DATABASE CONNECTION: FAILED");
					logger.error("   Connection is null or invalid");
					logger.error("⚠️  Application may not function correctly without database connectivity");
				}
			}
			
		} catch (Exception e) {
			logger.error("❌ DATABASE CONNECTION: FAILED");
			logger.error("   Error: {}", e.getMessage());
			logger.error("⚠️  Application startup completed but database is not accessible");
			logger.error("   Please check your database configuration and ensure the database server is running");
		}
		
		logger.info("===============================================");
	}
}
