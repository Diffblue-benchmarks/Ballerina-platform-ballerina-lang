/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ballerinalang.database.sql;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.BLangVMErrors;
import org.ballerinalang.connector.api.BLangConnectorSPIUtil;
import org.ballerinalang.model.types.BTypes;
import org.ballerinalang.model.types.TypeTags;
import org.ballerinalang.model.values.BError;
import org.ballerinalang.model.values.BInteger;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BRefType;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.util.exceptions.BallerinaException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Struct;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.Calendar;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class contains utility methods for SQL Connector operations.
 *
 * @since 0.8.0
 */
public class SQLDatasourceUtils {

    private static final String POOL_MAP_KEY = UUID.randomUUID().toString();

    /**
     * This will retrieve the string value for the given clob.
     *
     * @param data clob data
     * @return string value
     */
    public static String getString(Clob data) {
        if (data == null) {
            return null;
        }
        try (Reader r = new BufferedReader(data.getCharacterStream())) {
            StringBuilder sb = new StringBuilder();
            int pos;
            while ((pos = r.read()) != -1) {
                sb.append((char) pos);
            }
            return sb.toString();
        } catch (IOException | SQLException e) {
            throw new BallerinaException("error occurred while reading clob value: " + e.getMessage(), e);
        }
    }

    /**
     * This will retrieve the string value for the given blob.
     *
     * @param data blob data
     * @return string value
     */
    public static String getString(Blob data) {
        // Directly allocating full length arrays for decode byte arrays since anyway we are building
        // new String in memory.
        // Position of the getBytes has to be 1 instead of 0.
        // "pos - the ordinal position of the first byte in the BLOB value to be extracted;
        // the first byte is at position 1"
        // - https://docs.oracle.com/javase/7/docs/api/java/sql/Blob.html#getBytes(long,%20int)
        if (data == null) {
            return null;
        }
        try {
            byte[] encode = getBase64Encode(
                    new String(data.getBytes(1L, (int) data.length()), Charset.defaultCharset()));
            return new String(encode, Charset.defaultCharset());
        } catch (SQLException e) {
            throw new BallerinaException("error occurred while reading blob value", e);
        }
    }

    /**
     * This will retrieve the string value for the given binary data.
     *
     * @param data binary data
     * @return string value
     */
    public static String getString(byte[] data) {
        if (data == null) {
            return null;
        } else {
            return new String(data, Charset.defaultCharset());
        }
    }

    public static String getString(java.util.Date value) {
        if (value == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        String type;
        if (value instanceof Time) {
            calendar.setTimeInMillis(value.getTime());
            type = "time";
        } else if (value instanceof Timestamp) {
            calendar.setTimeInMillis(value.getTime());
            type = "datetime";
        } else if (value instanceof Date) {
            calendar.setTime(value);
            type = "date";
        } else {
            calendar.setTime(value);
            type = "time";
        }
        return getString(calendar, type);
    }

    /**
     * This will retrieve the string value for the given array data.
     *
     * @param dataArray data
     * @return string value
     * @throws SQLException sql exception when reading result set
     */
    public static String getString(Array dataArray) throws SQLException {
        if (dataArray == null) {
            return null;
        }
        StringJoiner sj = new StringJoiner(",", "[", "]");
        ResultSet rs = dataArray.getResultSet();
        while (rs.next()) {
            Object arrayEl = rs.getObject(2);
            String val = String.valueOf(arrayEl);
            sj.add(val);
        }
        return sj.toString();
    }

    /**
     * This will retrieve the string value for the given struct data.
     *
     * @param udt struct
     * @return string value
     * @throws SQLException sql exception when reading result set
     */
    public static String getString(Struct udt) throws SQLException {
        if (udt.getAttributes() != null) {
            StringJoiner sj = new StringJoiner(",", "{", "}");
            Object[] udtValues = udt.getAttributes();
            for (Object obj : udtValues) {
                sj.add(String.valueOf(obj));
            }
            return sj.toString();
        }
        return null;
    }

    public static BMap<String, BValue> createServerBasedDBClient(Context context, String dbType,
            BMap<String, BValue> clientEndpointConfig, String urlOptions,
            BMap<String, BRefType> globalPoolOptions) {
        String host = clientEndpointConfig.get(Constants.EndpointConfig.HOST).stringValue();
        int port = (int) ((BInteger) clientEndpointConfig.get(Constants.EndpointConfig.PORT)).intValue();
        String name = clientEndpointConfig.get(Constants.EndpointConfig.NAME).stringValue();
        String username = clientEndpointConfig.get(Constants.EndpointConfig.USERNAME).stringValue();
        String password = clientEndpointConfig.get(Constants.EndpointConfig.PASSWORD).stringValue();
        BMap<String, BRefType> poolOptions = (BMap<String, BRefType>) clientEndpointConfig
                .get(Constants.EndpointConfig.POOL_OPTIONS);
        boolean userProvidedPoolOptionsNotPresent = poolOptions == null;
        if (userProvidedPoolOptionsNotPresent) {
            poolOptions = globalPoolOptions;
        }
        PoolOptionsWrapper poolOptionsWrapper = new PoolOptionsWrapper(poolOptions);
        String jdbcUrl = constructJDBCURL(dbType, host, port, name, username, password, urlOptions);
        SQLDatasource.SQLDatasourceParamsBuilder builder = new SQLDatasource.SQLDatasourceParamsBuilder(dbType);
        SQLDatasource.SQLDatasourceParams sqlDatasourceParams = builder.withJdbcUrl(jdbcUrl)
                .withPoolOptions(poolOptionsWrapper).withUsername(username).withPassword(password).withDbName(name)
                .withIsGlobalDatasource(userProvidedPoolOptionsNotPresent).build();
        return createSQLClient(context, sqlDatasourceParams);
    }

    public static BMap<String, BValue> createSQLDBClient(Context context, BMap<String, BValue> clientEndpointConfig,
            BMap<String, BRefType> globalPoolOptions) {
        String url = clientEndpointConfig.get(Constants.EndpointConfig.URL).stringValue();
        String username = clientEndpointConfig.get(Constants.EndpointConfig.USERNAME).stringValue();
        String password = clientEndpointConfig.get(Constants.EndpointConfig.PASSWORD).stringValue();
        BMap<String, BRefType> dbOptions = (BMap<String, BRefType>) clientEndpointConfig
                .get(Constants.EndpointConfig.DB_OPTIONS);
        BMap<String, BRefType> poolOptions = (BMap<String, BRefType>) clientEndpointConfig
                .get(Constants.EndpointConfig.POOL_OPTIONS);
        boolean userProvidedPoolOptionsNotPresent = poolOptions == null;
        if (userProvidedPoolOptionsNotPresent) {
            poolOptions = globalPoolOptions;
        }
        PoolOptionsWrapper poolOptionsWrapper = new PoolOptionsWrapper(poolOptions);
        String dbType = url.split(":")[1].toUpperCase(Locale.getDefault());

        SQLDatasource.SQLDatasourceParamsBuilder builder = new SQLDatasource.SQLDatasourceParamsBuilder(dbType);
        SQLDatasource.SQLDatasourceParams sqlDatasourceParams = builder.withJdbcUrl("")
                .withPoolOptions(poolOptionsWrapper).withJdbcUrl(url).withUsername(username).withPassword(password)
                .withDbName("").withDbOptionsMap(dbOptions).withIsGlobalDatasource(userProvidedPoolOptionsNotPresent)
                .build();

        return createSQLClient(context, sqlDatasourceParams);
    }

    public static BMap<String, BValue> createMultiModeDBClient(Context context, String dbType,
            BMap<String, BValue> clientEndpointConfig, String urlOptions,
            BMap<String, BRefType> globalPoolOptions) {
        String modeRecordType = clientEndpointConfig.getType().getName();
        String dbPostfix = Constants.SQL_MEMORY_DB_POSTFIX;
        String hostOrPath = "";
        int port = -1;
        if (modeRecordType.equals(Constants.SERVER_MODE)) {
            dbPostfix = Constants.SQL_SERVER_DB_POSTFIX;
            hostOrPath = clientEndpointConfig.get(Constants.EndpointConfig.HOST).stringValue();
            port = (int) ((BInteger) clientEndpointConfig.get(Constants.EndpointConfig.PORT)).intValue();
        } else if (modeRecordType.equals(Constants.EMBEDDED_MODE)) {
            dbPostfix = Constants.SQL_FILE_DB_POSTFIX;
            hostOrPath = clientEndpointConfig.get(Constants.EndpointConfig.PATH).stringValue();;
        }
        dbType = dbType + dbPostfix;
        String name = clientEndpointConfig.get(Constants.EndpointConfig.NAME).stringValue();
        String username = clientEndpointConfig.get(Constants.EndpointConfig.USERNAME).stringValue();
        String password = clientEndpointConfig.get(Constants.EndpointConfig.PASSWORD).stringValue();
        BMap<String, BRefType> poolOptions = (BMap<String, BRefType>) clientEndpointConfig
                .get(Constants.EndpointConfig.POOL_OPTIONS);
        boolean userProvidedPoolOptionsNotPresent = poolOptions == null;
        if (userProvidedPoolOptionsNotPresent) {
            poolOptions = globalPoolOptions;
        }
        PoolOptionsWrapper poolOptionsWrapper = new PoolOptionsWrapper(poolOptions);
        SQLDatasource.SQLDatasourceParamsBuilder builder = new SQLDatasource.SQLDatasourceParamsBuilder(dbType);
        String jdbcUrl = constructJDBCURL(dbType, hostOrPath, port, name, username, password, urlOptions);
        SQLDatasource.SQLDatasourceParams sqlDatasourceParams = builder.withPoolOptions(poolOptionsWrapper)
                .withJdbcUrl(jdbcUrl).withDbType(dbType).withUsername(username).withPassword(password).withDbName(name)
                .withIsGlobalDatasource(userProvidedPoolOptionsNotPresent).build();
        return createSQLClient(context, sqlDatasourceParams);
    }

    public static String createJDBCDbOptions(String propertiesBeginSymbol, String separator,
            BMap<String, BRefType> dbOptions) {
        StringJoiner dbOptionsStringJoiner = new StringJoiner(separator, propertiesBeginSymbol, "");
        dbOptions.getMap().forEach((key, value) -> {
            if (isSupportedDbOptionType(value)) {
                dbOptionsStringJoiner
                        .add(key + Constants.JDBCUrlSeparators.EQUAL_SYMBOL + value.value());
            } else {
                throw new BallerinaException("Unsupported type for the db option: " + key);
            }
        });
        return dbOptionsStringJoiner.toString();
    }


    public static BError getSQLConnectorError(Context context, Throwable throwable, String messagePrefix) {
        String detailedErrorMessage =
                throwable.getMessage() != null ? throwable.getMessage() : Constants.DATABASE_ERROR_MESSAGE;
        return getSQLConnectorError(context, messagePrefix + detailedErrorMessage);
    }

    public static BError getSQLConnectorError(Context context, String detailedErrorMessage) {
        BMap<String, BValue> sqlClientErrorDetailRecord = BLangConnectorSPIUtil
                .createBStruct(context, Constants.SQL_PACKAGE_PATH, Constants.DATABASE_ERROR_DATA_RECORD_NAME,
                        detailedErrorMessage);
        return BLangVMErrors.createError(context, true, BTypes.typeError, Constants.DATABASE_ERROR_CODE,
                sqlClientErrorDetailRecord);
    }

    protected static ConcurrentHashMap<String, SQLDatasource> retrieveDatasourceContainer(
            BMap<String, BRefType> poolOptions) {
        return (ConcurrentHashMap<String, SQLDatasource>) poolOptions.getNativeData(POOL_MAP_KEY);
    }

    protected static void addDatasourceContainer(BMap<String, BRefType> poolOptions,
            ConcurrentHashMap<String, SQLDatasource> datasourceMap) {
        poolOptions.addNativeData(POOL_MAP_KEY, datasourceMap);
    }

    protected static boolean isSupportedDbOptionType(BValue value) {
        boolean supported = false;
        if (value != null) {
            int typeTag = value.getType().getTag();
            supported = (typeTag == TypeTags.STRING_TAG || typeTag == TypeTags.INT_TAG || typeTag == TypeTags.FLOAT_TAG
                    || typeTag == TypeTags.BOOLEAN_TAG || typeTag == TypeTags.DECIMAL_TAG
                    || typeTag == TypeTags.BYTE_TAG);
        }
        return supported;
    }

    private static String constructJDBCURL(String dbType, String hostOrPath, int port, String dbName, String username,
            String password, String dbOptions) {
        StringBuilder jdbcUrl = new StringBuilder();
        dbType = dbType.toUpperCase(Locale.ENGLISH);
        hostOrPath = hostOrPath.replaceAll("/$", "");
        switch (dbType) {
        case Constants.DBTypes.MYSQL:
            if (port <= 0) {
                port = Constants.DefaultPort.MYSQL;
            }
            jdbcUrl.append("jdbc:mysql://").append(hostOrPath).append(":").append(port).append("/").append(dbName);
            break;
        case Constants.DBTypes.SQLSERVER:
            if (port <= 0) {
                port = Constants.DefaultPort.SQLSERVER;
            }
            jdbcUrl.append("jdbc:sqlserver://").append(hostOrPath).append(":").append(port).append(";databaseName=")
                    .append(dbName);
            break;
        case Constants.DBTypes.ORACLE:
            if (port <= 0) {
                port = Constants.DefaultPort.ORACLE;
            }
            jdbcUrl.append("jdbc:oracle:thin:").append(username).append("/").append(password).append("@")
                    .append(hostOrPath).append(":").append(port).append("/").append(dbName);
            break;
        case Constants.DBTypes.SYBASE:
            if (port <= 0) {
                port = Constants.DefaultPort.SYBASE;
            }
            jdbcUrl.append("jdbc:sybase:Tds:").append(hostOrPath).append(":").append(port).append("/").append(dbName);
            break;
        case Constants.DBTypes.POSTGRESQL:
            if (port <= 0) {
                port = Constants.DefaultPort.POSTGRES;
            }
            jdbcUrl.append("jdbc:postgresql://").append(hostOrPath).append(":").append(port).append("/").append(dbName);
            break;
        case Constants.DBTypes.IBMDB2:
            if (port <= 0) {
                port = Constants.DefaultPort.IBMDB2;
            }
            jdbcUrl.append("jdbc:db2:").append(hostOrPath).append(":").append(port).append("/").append(dbName);
            break;
        case Constants.DBTypes.HSQLDB_SERVER:
            if (port <= 0) {
                port = Constants.DefaultPort.HSQLDB_SERVER;
            }
            jdbcUrl.append("jdbc:hsqldb:hsql://").append(hostOrPath).append(":").append(port).append("/")
                    .append(dbName);
            break;
        case Constants.DBTypes.HSQLDB_FILE:
            jdbcUrl.append("jdbc:hsqldb:file:").append(hostOrPath).append(File.separator).append(dbName);
            break;
        case Constants.DBTypes.H2_SERVER:
            if (port <= 0) {
                port = Constants.DefaultPort.H2_SERVER;
            }
            jdbcUrl.append("jdbc:h2:tcp:").append(hostOrPath).append(":").append(port).append("/").append(dbName);
            break;
        case Constants.DBTypes.H2_FILE:
            jdbcUrl.append("jdbc:h2:file:").append(hostOrPath).append(File.separator).append(dbName);
            break;
        case Constants.DBTypes.H2_MEMORY:
            jdbcUrl.append("jdbc:h2:mem:").append(dbName);
            break;
        case Constants.DBTypes.DERBY_SERVER:
            if (port <= 0) {
                port = Constants.DefaultPort.DERBY_SERVER;
            }
            jdbcUrl.append("jdbc:derby:").append(hostOrPath).append(":").append(port).append("/").append(dbName);
            break;
        case Constants.DBTypes.DERBY_FILE:
            jdbcUrl.append("jdbc:derby:").append(hostOrPath).append(File.separator).append(dbName);
            break;
        default:
            throw new BallerinaException("cannot generate url for unknown database type : " + dbType);
        }
        return dbOptions.isEmpty() ? jdbcUrl.toString() : jdbcUrl.append(dbOptions).toString();
    }

    private static BMap<String, BValue> createSQLClient(Context context,
            SQLDatasource.SQLDatasourceParams sqlDatasourceParams) {
        SQLDatasource sqlDatasource = sqlDatasourceParams.getPoolOptionsWrapper()
                .retrieveDatasource(sqlDatasourceParams);
        BMap<String, BValue> sqlClient = BLangConnectorSPIUtil
                .createBStruct(context.getProgramFile(), Constants.SQL_PACKAGE_PATH, Constants.SQL_CLIENT);
        sqlClient.addNativeData(Constants.SQL_CLIENT, sqlDatasource);
        return sqlClient;
    }

    private static String getString(Calendar calendar, String type) {
        if (!calendar.isSet(Calendar.ZONE_OFFSET)) {
            calendar.setTimeZone(TimeZone.getDefault());
        }
        StringBuffer datetimeString = new StringBuffer(28);
        switch (type) {
        case "date": //'-'? yyyy '-' mm '-' dd zzzzzz?
            appendDate(datetimeString, calendar);
            appendTimeZone(calendar, datetimeString);
            break;
        case "time": //hh ':' mm ':' ss ('.' s+)? (zzzzzz)?
            appendTime(calendar, datetimeString);
            appendTimeZone(calendar, datetimeString);
            break;
        case "datetime": //'-'? yyyy '-' mm '-' dd 'T' hh ':' mm ':' ss ('.' s+)? (zzzzzz)?
            appendDate(datetimeString, calendar);
            datetimeString.append("T");
            appendTime(calendar, datetimeString);
            appendTimeZone(calendar, datetimeString);
            break;
        default:
            throw new BallerinaException("invalid type for datetime data: " + type);
        }
        return datetimeString.toString();
    }

    private static byte[] getBase64Encode(String st) {
        return Base64.getEncoder().encode(st.getBytes(Charset.defaultCharset()));
    }

    private static void appendTimeZone(Calendar calendar, StringBuffer dateString) {
        int timezoneOffSet = calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET);
        int timezoneOffSetInMinits = timezoneOffSet / 60000;
        if (timezoneOffSetInMinits < 0) {
            dateString.append("-");
            timezoneOffSetInMinits = timezoneOffSetInMinits * -1;
        } else {
            dateString.append("+");
        }
        int hours = timezoneOffSetInMinits / 60;
        int minits = timezoneOffSetInMinits % 60;
        if (hours < 10) {
            dateString.append("0");
        }
        dateString.append(hours).append(":");
        if (minits < 10) {
            dateString.append("0");
        }
        dateString.append(minits);
    }

    private static void appendTime(Calendar value, StringBuffer dateString) {
        if (value.get(Calendar.HOUR_OF_DAY) < 10) {
            dateString.append("0");
        }
        dateString.append(value.get(Calendar.HOUR_OF_DAY)).append(":");
        if (value.get(Calendar.MINUTE) < 10) {
            dateString.append("0");
        }
        dateString.append(value.get(Calendar.MINUTE)).append(":");
        if (value.get(Calendar.SECOND) < 10) {
            dateString.append("0");
        }
        dateString.append(value.get(Calendar.SECOND)).append(".");
        if (value.get(Calendar.MILLISECOND) < 10) {
            dateString.append("0");
        }
        if (value.get(Calendar.MILLISECOND) < 100) {
            dateString.append("0");
        }
        dateString.append(value.get(Calendar.MILLISECOND));
    }

    private static void appendDate(StringBuffer dateString, Calendar calendar) {
        int year = calendar.get(Calendar.YEAR);
        if (year < 1000) {
            dateString.append("0");
        }
        if (year < 100) {
            dateString.append("0");
        }
        if (year < 10) {
            dateString.append("0");
        }
        dateString.append(year).append("-");
        // sql date month is started from 1 and calendar month is
        // started from 0. so have to add one
        int month = calendar.get(Calendar.MONTH) + 1;
        if (month < 10) {
            dateString.append("0");
        }
        dateString.append(month).append("-");
        if (calendar.get(Calendar.DAY_OF_MONTH) < 10) {
            dateString.append("0");
        }
        dateString.append(calendar.get(Calendar.DAY_OF_MONTH));
    }
}
