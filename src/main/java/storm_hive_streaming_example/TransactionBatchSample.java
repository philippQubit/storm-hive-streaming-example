package storm_hive_streaming_example;

import java.util.ArrayList;

import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hive.hcatalog.streaming.DelimitedInputWriter;
import org.apache.hive.hcatalog.streaming.HiveEndPoint;
import org.apache.hive.hcatalog.streaming.StreamingConnection;
import org.apache.hive.hcatalog.streaming.TransactionBatch;

/**
 * Created by philipp on 09/02/15.
 */
public class TransactionBatchSample {

    /*
     Needs the following table created in hive:
      CREATE TABLE alerts ( id int, msg string) PARTITIONED BY (continent string, country string)
      CLUSTERED BY (id) INTO 5 buckets stored as orc;

    After insert use this:
     USE testing;
     SET hive.input.format=org.apache.hadoop.hive.ql.io.HiveInputFormat;

     ﻿ALTER TABLE alerts PARTITION(continent='Asia',country='India') COMPACT 'minor';
     SELECT id,msg FROM alerts;

     ﻿ALTER TABLE alerts PARTITION(continent='Asia',country='India') COMPACT 'major';
     SELECT id,msg FROM alerts;
     */

    public static void main(String... args) {
        //-------   MAIN THREAD  ------- //
        String dbName = "testing";
        String tblName = "alerts";
        ArrayList<String> partitionVals = new ArrayList<String>(2);
        partitionVals.add("Asia");
        partitionVals.add("India");
        String serdeClass = "org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe";

        HiveEndPoint hiveEP = new HiveEndPoint("thrift://localhost:9083", dbName, tblName, partitionVals);

        try {
            HiveConf conf = new HiveConf();
            conf.verifyAndSet("hive.txn.manager", "org.apache.hadoop.hive.ql.lockmgr.DbTxnManager");
            conf.verifyAndSet("hive.support.concurrency", "true");
            conf.verifyAndSet("hive.metastore.execute.setugi", "true");
            conf.verifyAndSet("hive.execution.engine", "mr");

            StreamingConnection connection = hiveEP.newConnection(true, conf);
            DelimitedInputWriter writer = new DelimitedInputWriter(new String[] { "id", "msg"}, ",", hiveEP, conf);
            TransactionBatch txnBatch = connection.fetchTransactionBatch(10, writer);

            ///// Batch 1 - First TXN
            txnBatch.beginNextTransaction();
            txnBatch.write("1,Hello streaming".getBytes());
            txnBatch.write("2,Welcome to streaming".getBytes());
            txnBatch.commit();

            if(txnBatch.remainingTransactions() > 0) {
                ///// Batch 1 - Second TXN
                txnBatch.beginNextTransaction();
                txnBatch.write("3,Roshan Naik".getBytes());
                txnBatch.write("4,Alan Gates".getBytes());
                txnBatch.write("5,Owen O’Malley".getBytes());
                txnBatch.commit();


                txnBatch.close();
                connection.close();
            }

            /*
            txnBatch = connection.fetchTransactionBatch(10, writer);

            ///// Batch 2 - First TXN
            txnBatch.beginNextTransaction();
            txnBatch.write("6,David Schorow".getBytes());
            txnBatch.write("7,Sushant Sowmyan".getBytes());
            txnBatch.commit();


            if(txnBatch.remainingTransactions() > 0) {
                ///// Batch 2 - Second TXN
                txnBatch.beginNextTransaction();
                txnBatch.write("8,Ashutosh Chauhan".getBytes());
                txnBatch.write("9,Thejas Nair".getBytes());
                txnBatch.commit();


                txnBatch.close();
            }*/

            //connection.close();
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }
}
