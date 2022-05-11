package example.dao;
/**
 * Copyright 2022 Serguei Kouzmine
 */

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import example.controller.NodeExporterController;
import example.entity.Host;
import example.utils.JDBCUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;

@Repository("JdbcDao")
public class JDBCDao implements Dao {

	// http://www.baeldung.com/properties-with-spring
	// Autowired annotation is not supported on static fields
	@Value("${spring.datasource.url}")
	private String datasourceUrl;
	private static final Logger logger = LogManager
			.getLogger(JDBCDao.class.getName());

	private static final Connection conn = JDBCUtils.getConnection();

	@Override
	public int addHost(Host host) {
		int result = 0;
		String sql = "INSERT INTO hosts(hostname,appid,environment,datacenter) VALUES (?,?,?,?)";
		try {
			PreparedStatement preparedStatement = conn.prepareStatement(sql);
			preparedStatement.setString(1, host.getHostname());
			preparedStatement.setString(2, host.getAppid());
			preparedStatement.setString(3, host.getEnvironment());
			preparedStatement.setString(4, host.getDatacenter());
			result = preparedStatement.executeUpdate();
		} catch (Exception e) {
			logger.error(e.toString());
		}
		return result;
	}

	@Override
	public List<?> findAllHost() {
		logger.info("datasourceUrl = " + datasourceUrl);
		List<?> results = null;
		String sql = "SELECT * FROM hosts";
		try {
			PreparedStatement preparedStatement = conn.prepareStatement(sql);
			ResultSet resultSet = preparedStatement.executeQuery();
			results = JDBCUtils.TranverseToList(resultSet, Host.class);
		} catch (Exception e) {
			logger.error(e.toString());
		}
		return results;
	}

	@Override
	public int updateHost(Host host) {
		int result = 0;
		String sql = "UPDATE hosts SET hostname = ?,appid = ?, environment = ?, datacenter = ? WHERE id = ?";
		try {
			PreparedStatement preparedStatement = conn.prepareStatement(sql);

			preparedStatement.setString(1, host.getHostname());
			preparedStatement.setString(2, host.getAppid());
			preparedStatement.setString(3, host.getEnvironment());
			preparedStatement.setString(4, host.getDatacenter());
			preparedStatement.setLong(3, host.getId());
			result = preparedStatement.executeUpdate();
		} catch (Exception e) {
			logger.error(e.toString());
		}
		return result;
	}

	@Override
	public int delHostById(long id) {
		int result = 0;
		String sql = "DELETE FROM hosts WHERE id = ?";
		try {
			PreparedStatement preparedStatement = conn.prepareStatement(sql);
			preparedStatement.setLong(1, id);
			result = preparedStatement.executeUpdate();
		} catch (Exception e) {
			logger.error(e.toString());
		}
		return result;
	}

	@Override
	public Host findHostById(long id) {
		List<?> results = null;
		Host result = null;
		String sql = "SELECT * FROM hosts WHERE id = ?";
		try {
			PreparedStatement preparedStatement = conn.prepareStatement(sql);
			preparedStatement.setLong(1, id);
			ResultSet resultSet = preparedStatement.executeQuery();
			results = JDBCUtils.TranverseToList(resultSet, Host.class);
			// probably unnecessary, shown as example
			// https://stackoverflow.com/questions/12320429/java-how-to-check-the-type-of-an-arraylist-as-a-whole
			if (results != null && results instanceof List<?>
					&& results.size() != 0) {
				result = (Host) results.get(0);
			}
		} catch (SQLException | InstantiationException | IllegalAccessException e) {
			logger.error(e.toString());
		}
		return result;
	}

	@Override
	public Host findHostByHostname(String hostname) {
		List<?> results = null;
		Host result = null;
		logger.info("Selecting data for " + hostname);
		String sql = "SELECT * FROM hosts WHERE hostname = ?";
		try {
			PreparedStatement preparedStatement = conn.prepareStatement(sql);
			preparedStatement.setString(1, hostname);
			logger.info("in select");
			ResultSet resultSet = preparedStatement.executeQuery();
			results = JDBCUtils.TranverseToList(resultSet, Host.class);
			logger.info("in select:  " + results);
			// probably unnecesary, shown as example
			// https://stackoverflow.com/questions/12320429/java-how-to-check-the-type-of-an-arraylist-as-a-whole
			if (results != null && results instanceof List<?>
					&& results.size() != 0) {
				result = (Host) results.get(0);
			}
		} catch (SQLException | InstantiationException | IllegalAccessException e) {
			logger.error(e.toString());
		}
		return result;
	}

	// mysql required connectionn string patch:
	// dataSource.setUrl("jdbc:mysql://localhost:3306/userdb" +
	// "?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC");
	// https://github.com/Pragmatists/JUnitParams
	// http://www.cyberforum.ru/java-j2ee/thread2160223.html
	// for custom DAO implementing security tokens see
	// https://github.com/sebasv89/spring-boot-examples/tree/master/src/main/java/co/svelez/springbootexample/domain
	// see also:
	// ttps://stackoverflow.com/questions/36261216/how-to-rename-the-table-persistent-logins-in-spring-security
	// https://qna.habr.com/q/855545
}
