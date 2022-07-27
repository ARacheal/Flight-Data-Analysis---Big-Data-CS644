package flightproject;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/*******
CS 644	final project
Members
Jaime Corea
Antonita Rachael 
Surbhi Patel
Soumya Sharma
********/

public class flightproject {

     //Mapper for flight
	public static class MapperOne extends Mapper<LongWritable, Text, Text, Text> {

		protected void map(LongWritable key, Text value, Mapper<LongWritable, Text, Text, Text>.Context context)
				throws IOException, InterruptedException {

			String[] line = value.toString().split(",");
			if (line[14].trim().matches("-?[0-9]+")) {
				int delay_val = Integer.valueOf(line[14]);
				if (delay_val >= -5 && delay_val <= 5) {
					context.write(new Text(line[8].trim() + line[9].trim()), new Text("1"));
				} else {
					context.write(new Text(line[8].trim() + line[9].trim()), new Text("0"));
				}
			}
		}

	}

	//Reducer for flight
	public static class ReducerOne extends Reducer<Text, Text, Text, Text> {

		LinkedList<Flight> highest_schedules = new LinkedList<>();
		LinkedList<Flight> lowest_schedules = new LinkedList<>();

		class Flight implements Comparable<Flight> {
			String flight_no;
			double onTime;

			public Flight(String relative, double percentage) {
				this.flight_no = relative;
				this.onTime = percentage;
			}

			@Override
			public int compareTo(Flight flight) {
				if (this.onTime >= flight.onTime) {
					return -1;
				} else {
					return 1;
				}
			}
		}

		protected void reduce(Text key, Iterable<Text> values, Reducer<Text, Text, Text, Text>.Context context)
				throws IOException, InterruptedException {
			int total = 0;
			int onTime = 0;
			for (Text value : values) {
				total++;
				if (value.toString().equals("1"))
					onTime++;
			}
			double perctg = (double) onTime / total * 100;
			Flight mFlight = new Flight(key.toString(), perctg);
			Top3onSchedule(mFlight);
			Bottom3onSchedule(mFlight);
		}

		private void Top3onSchedule(Flight f) {
			if (highest_schedules.size() < 3)
				highest_schedules.add(f);
			else {
				highest_schedules.add(f);
				Collections.sort(highest_schedules);
				highest_schedules.removeLast();
			}
		}

		private void Bottom3onSchedule(Flight f) {
			if (lowest_schedules.size() < 3)
				lowest_schedules.add(f);
			else {
				lowest_schedules.add(f);
				Collections.sort(lowest_schedules);
				Collections.reverse(lowest_schedules);
				lowest_schedules.removeLast();
			}
		}

		protected void cleanup(Context context) throws IOException, InterruptedException {
			context.write(new Text("Top 3 airlines on schedule:"), new Text(""));
			for (Flight f : highest_schedules) {
				context.write(new Text(f.flight_no), new Text(String.valueOf(f.onTime) + "%"));
			}
			context.write(new Text("Bottom 3 airlines on schedule:"), new Text(""));
			for (Flight f : lowest_schedules) {
				context.write(new Text(f.flight_no), new Text(String.valueOf(f.onTime) + "%"));
			}
		}
	}

	// Mapper for Taxi
	public static class MapperTwo extends Mapper<LongWritable, Text, Text, Text> {

		protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

			String[] line = value.toString().split(",");
			if (line[19].trim().matches("-?[0-9]+") && !line[19].trim().equals("")) {
				context.write(new Text(line[17]), new Text(line[19]));
			}
			if (line[20].trim().matches("-?[0-9]+") && !line[20].trim().equals("")) {
				context.write(new Text(line[16]), new Text(line[20]));
			}
		}
	}

	// Reducer for taxi
	public static class ReducerTwo extends Reducer<Text, Text, Text, Text> {
		LinkedList<Airport> Top3taxiFlight = new LinkedList<>();
		LinkedList<Airport> Bottom3taxiFlight = new LinkedList<>();

		class Airport implements Comparable<Airport> {
			String airport;
			double aveTaxi;

			public Airport(String str, double av) {
				this.airport = str;
				this.aveTaxi = av;
			}

			@Override
			public int compareTo(Airport apt) {
				if (this.aveTaxi >= apt.aveTaxi) {
					return -1;
				} else {
					return 1;
				}
			}

		}

		protected void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			double totalTime = 0;
			long number = 0;
			for (Text t : values) {
				number++;
				totalTime = +Math.abs(Integer.valueOf(t.toString()));
			}
			if (number > 0) {
				double avg = (double) totalTime / number;
				Airport mF = new Airport(key.toString(), avg);
				TaxiFlightTop3(mF);
				TaxiFlightBottom3(mF);
			}
		}

		@Override
		protected void cleanup(Context context) throws IOException, InterruptedException {
			context.write(new Text("Top 3 average taxi time per flight:"), new Text(""));
			for (Airport f : Top3taxiFlight) {
				context.write(new Text(f.airport), new Text(String.valueOf(f.aveTaxi)));
			}
			context.write(new Text("Bottom 3 average taxi time per flight:"), new Text(""));
			for (Airport f : Bottom3taxiFlight) {
				context.write(new Text(f.airport), new Text(String.valueOf(f.aveTaxi)));
			}
		}

		private void TaxiFlightTop3(Airport f) {
			if (Top3taxiFlight.size() < 3)
				Top3taxiFlight.add(f);
			else {
				Top3taxiFlight.add(f);
				Collections.sort(Top3taxiFlight);
				Top3taxiFlight.removeLast();
			}
		}

		private void TaxiFlightBottom3(Airport f) {
			if (Bottom3taxiFlight.size() < 3)
				Bottom3taxiFlight.add(f);
			else {
				Bottom3taxiFlight.add(f);
				Collections.sort(Bottom3taxiFlight);
				Collections.reverse(Bottom3taxiFlight);
				Bottom3taxiFlight.removeLast();
			}
		}
	}

	// Mapper for cancellation
	public static class MapperThree extends Mapper<LongWritable, Text, Text, Text> {

		protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			// TODO Auto-generated method stub
			String[] line = value.toString().split(",");
			String cancelCode = line[22].trim();
			if (line[0] != null && !line[0].trim().equals("Year")) {
				if (cancelCode != null && !cancelCode.equals("") && !cancelCode.equals("NA")) {
					context.write(new Text(cancelCode), new Text("1"));
				}
			}
		}
	}

	// Reducer for cancellation
	public static class ReducerThree extends Reducer<Text, Text, Text, Text> {
		private Text result = new Text();

		int max = 0;
		Text maxCode = new Text();

		protected void reduce(Text key, Iterable<Text> values, Reducer<Text, Text, Text, Text>.Context arg2)
				throws IOException, InterruptedException {
			// TODO Auto-generated method stub
			int sum = 0;
			for (Text v : values) {
				sum += Integer.valueOf(v.toString());
			}

			if (sum >= max) {
				maxCode.set(key);
				max = sum;
			}

			result.set(String.valueOf(sum));
		}

		@Override
		protected void cleanup(Context context) throws IOException, InterruptedException {
			context.write(new Text("Flight cancellations most frequent reason:"), new Text());
			context.write(maxCode, new Text(String.valueOf(max)));
		}

	}

// driver class main
	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		Job job1 = Job.getInstance(conf, "Job1");
		job1.setJarByClass(flightproject.class);
		job1.setMapperClass(MapperOne.class);
		job1.setReducerClass(ReducerOne.class);
		job1.setOutputKeyClass(Text.class);
		job1.setOutputValueClass(Text.class);
		job1.setNumReduceTasks(1);
		FileSystem fs = FileSystem.get(conf);
		FileInputFormat.addInputPath(job1, new Path(args[0]));
		if (fs.exists(new Path(args[1] + "_FlightsOnSchedule"))) {
			fs.delete(new Path(args[1] + "_FlightsOnSchedule"), true);
		}
		FileOutputFormat.setOutputPath(job1, new Path(args[1] + "_FlightsOnSchedule"));
		if (job1.waitForCompletion(true)) {
			Job job2 = Job.getInstance(conf, "Job2");
			job2.setJarByClass(flightproject.class);
			job2.setMapperClass(MapperTwo.class);
			job2.setReducerClass(ReducerTwo.class);
			job2.setOutputKeyClass(Text.class);
			job2.setOutputValueClass(Text.class);
			job2.setNumReduceTasks(1);
			FileInputFormat.addInputPath(job2, new Path(args[0]));
			if (fs.exists(new Path(args[1] + "_TaxiInTime"))) {
				fs.delete(new Path(args[1] + "_TaxiInTime"), true);
			}
			FileOutputFormat.setOutputPath(job2, new Path(args[1] + "_TaxiInTime"));
			if (job2.waitForCompletion(true)) {
				Job job3 = Job.getInstance(conf, "Job3");
				job3.setJarByClass(flightproject.class);
				job3.setMapperClass(MapperThree.class);
				job3.setReducerClass(ReducerThree.class);
				job3.setOutputKeyClass(Text.class);
				job3.setOutputValueClass(Text.class);
				job3.setNumReduceTasks(1);
				FileInputFormat.addInputPath(job3, new Path(args[0]));
				if (fs.exists(new Path(args[1] + "_FlightCancellation"))) {
					fs.delete(new Path(args[1] + "_FlightCancellation"), true);
				}
				FileOutputFormat.setOutputPath(job3, new Path(args[1] + "_FlightCancellation"));
				System.exit(job3.waitForCompletion(true) ? 0 : 1);
			}
		}
	}
}