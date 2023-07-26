package com.customer
import org.apache.spark.sql.catalyst.dsl.expressions
import org.apache.spark.sql.catalyst.dsl.expressions
import org.apache.spark.sql.catalyst.dsl.expressions
import org.apache.spark.sql.{SparkSession, functions}
import org.apache.spark.sql.functions.{col, date_format, lit, split, to_date}
import org.apache.spark.sql.functions._

object customer extends Serializable {
  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder()
      .appName("ecommerce")
      .master("local[3]")
      .config("spark.cassandra.connection.host", "localhost")
      .config("spark.cassandra.connection.port", "9042")
      .config("spark.sql.extensions", "com.datastax.spark.connector.CassandraSparkExtensions")
      .config("spark.sql.catalog.lh", "com.datastax.spark.connector.datasource.CassandraCatalog")
      .getOrCreate()
    var customerdf = spark.read.option("header", true).csv("C:\\poc\\customer\\Data_Customer_behaviour\\customer.csv")
    var purchasedf = spark.read.option("header", true).csv("C:\\poc\\customer\\Data_Customer_behaviour\\purchase.csv")
    var clickstreamdf = spark.read.option("header", true).csv("C:\\poc\\customer\\Data_Customer_behaviour\\click.csv")
    customerdf.show()
    purchasedf.show()
    clickstreamdf.show()

    val customer_1 = customerdf.select(functions.split(col("userID,name,email"), ",").getItem(0).as("userID"),
      functions.split(col("userID,name,email"), ",").getItem(1).as("name"),
      functions.split(col("userID,name,email"), ",").getItem(2).as("email"))
    //customer_1.show()
    val purchase_1 = purchasedf.select(functions.split(col("userID,timestamp,amount"), ",").getItem(0).as("userID"),
      functions.split(col("userID,timestamp,amount"), ",").getItem(1).as("timestamp"),
      functions.split(col("userID,timestamp,amount"), ",").getItem(2).as("amount"))
    //purchase_1.show()
    val click_1 = clickstreamdf.select(functions.split(col("userID,timestamp,page"), ",").getItem(0).as("userID"),
      functions.split(col("userID,timestamp,page"), ",").getItem(1).as("timestamp"),
      functions.split(col("userID,timestamp,page"), ",").getItem(2).as("page"))
    //click_1.show()

    //join query
    customer_1.createOrReplaceTempView("table1_view")
    purchase_1.createOrReplaceTempView("table2_view")
    val join_query = "select t1.userID,t1.name,t1.email,t2.timestamp,t2.amount from table1_view t1 join table2_view t2 on t1.userID=t2.userID"
    val join_result = spark.sql(join_query)
    join_result.show()

    val first_name = customer_1.withColumn("firstname", split(col("name"), " ").getItem(0))
    val last_name = first_name.withColumn("lastname", split(col("name"), " ").getItem(1))
    val newcustomer = last_name.withColumnRenamed("name", "full_name")
    newcustomer.show()



    val purchase_df = purchase_1.withColumn("date", to_date(col("timestamp")))
    val purchase_2 = purchase_df.withColumn("time", date_format(col("timestamp"), "HH:mm:ss"))
    val newpurchase = purchase_2.drop("timestamp")
    newpurchase.show()

    val click3 = click_1.orderBy(col("page").desc, col("userID").desc)
    click3.show()
    val click_df = click3.withColumn("date", to_date(col("timestamp")))
    val click_2 = click_df.withColumn("time", date_format(col("timestamp"), "HH:mm:ss"))
    val newclick = click_2.drop("timestamp")
    newclick.show()

    val new_customer = newcustomer.withColumnRenamed("userID", "userid")
    val new_purchase = newpurchase.withColumnRenamed("userID", "userid")
    val new_click = newclick.withColumnRenamed("userID", "userid")
    new_customer.write
      .format("org.apache.spark.sql.cassandra")
      .option("keyspace", "customer_behaviour")
      .option("table", "new_customer")
      .mode("append")
      .save()
    new_purchase.write
      .format("org.apache.spark.sql.cassandra")
      .option("keyspace", "customer_behaviour")
      .option("table", "new_purchase")
      .mode("append")
      .save()
    new_click.write
      .format("org.apache.spark.sql.cassandra")
      .option("keyspace", "customer_behaviour")
      .option("table", "new_click")
      .mode("append")
      .save()
  }
}




