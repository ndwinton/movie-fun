package org.superbiz.moviefun;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
public class Application {

    public static final String ALBUMS_QUALIFIER = "albums";
    public static final String MOVIES_QUALIFIER = "movies";

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public ServletRegistrationBean actionServletRegistration(ActionServlet actionServlet) {
        return new ServletRegistrationBean(actionServlet, "/moviefun/*");
    }

    @Bean
    DatabaseServiceCredentials databaseServiceCredentials() {
        return new DatabaseServiceCredentials(System.getenv("VCAP_SERVICES"));
    }

    @Bean
    @Qualifier(ALBUMS_QUALIFIER)
    public DataSource albumsDataSource(DatabaseServiceCredentials serviceCredentials) {
        return createdWrappedMysqlDataSource("albums-mysql", serviceCredentials);
    }

    @Bean
    @Qualifier(MOVIES_QUALIFIER)
    public DataSource moviesDataSource(DatabaseServiceCredentials serviceCredentials) {
        return createdWrappedMysqlDataSource("movies-mysql", serviceCredentials);
    }

    private DataSource createdWrappedMysqlDataSource(String name, DatabaseServiceCredentials serviceCredentials) {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setURL(serviceCredentials.jdbcUrl(name));
        HikariConfig config = new HikariConfig();
        config.setDataSource(dataSource);
        return new HikariDataSource(config);
    }

    @Bean
    HibernateJpaVendorAdapter hibernateJpaVendorAdapter() {
        HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
        adapter.setDatabase(Database.MYSQL);
        adapter.setDatabasePlatform("org.hibernate.dialect.MySQL5Dialect");
        adapter.setGenerateDdl(true);
        return adapter;
    }

    @Bean
    @Qualifier(MOVIES_QUALIFIER)
    LocalContainerEntityManagerFactoryBean moviesEntityManagerFactory(@Qualifier(MOVIES_QUALIFIER) DataSource dataSource, HibernateJpaVendorAdapter adapter) {
        return createFactoryBean(dataSource, adapter, "movies");
    }

    @Bean
    @Qualifier(ALBUMS_QUALIFIER)
    LocalContainerEntityManagerFactoryBean albumsEntityManagerFactory(@Qualifier(ALBUMS_QUALIFIER) DataSource dataSource, HibernateJpaVendorAdapter adapter) {
        return createFactoryBean(dataSource, adapter, "albums");
    }

    private LocalContainerEntityManagerFactoryBean createFactoryBean(DataSource dataSource, HibernateJpaVendorAdapter adapter, String name) {
        LocalContainerEntityManagerFactoryBean bean = new LocalContainerEntityManagerFactoryBean();
        bean.setDataSource(dataSource);
        bean.setJpaVendorAdapter(adapter);
        bean.setPackagesToScan("org.superbiz.moviefun." + name);
        bean.setPersistenceUnitName(name + "-unit");
        return bean;
    }

    @Bean
    @Qualifier(MOVIES_QUALIFIER)
    PlatformTransactionManager moviesPlatformTransactionManager(@Qualifier(MOVIES_QUALIFIER) EntityManagerFactory factory) {
        return new JpaTransactionManager(factory);
    }

    @Bean
    @Qualifier(ALBUMS_QUALIFIER)
    PlatformTransactionManager albumsPlatformTransactionManager(@Qualifier(ALBUMS_QUALIFIER) EntityManagerFactory factory) {
        return new JpaTransactionManager(factory);
    }
}
