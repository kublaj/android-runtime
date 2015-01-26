package com.tns;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

public class Async
{
	public interface CompleteCallback {
		void onComplete(Object result, Object context);
	}
	
	public static void DownloadImage(String url, CompleteCallback callback, Object context){
		new ImageDownloadTask(callback, context).execute(url);
	}
	
	public static class Http 
	{
		public static class KeyValuePair
		{
			public String key;
			public String value;
			
			public KeyValuePair(String key, String value)
			{
				this.key = key;
				this.value = value;
			}
		}
		
		public static class RequestOptions 
		{
			public String url;
			public String method;
			public ArrayList<KeyValuePair> headers;
			public String content;
			public int timeout = -1;
			
			public void addHeaders(HttpURLConnection connection)
			{
				if(this.headers == null)
				{
					return;
				}
				
				for (KeyValuePair pair : this.headers) {
				    connection.addRequestProperty(pair.key.toString(),  pair.value.toString());
				}
			}
			
			public void writeContent(HttpURLConnection connection, Stack<Closeable> openedStreams) throws IOException
			{
				if(this.content == null || this.content.getClass() != String.class)
				{
					return;
				}
				
				OutputStream outStream = connection.getOutputStream();
				openedStreams.push(outStream);
				
				OutputStreamWriter writer = new java.io.OutputStreamWriter(outStream);
				openedStreams.push(writer);
				
				writer.write((String)this.content);
			}
		}
		
		public static class RequestResult
		{
			public ByteArrayOutputStream raw;
			public ArrayList<KeyValuePair> headers = new ArrayList<KeyValuePair>();
			public int statusCode;
			public String responseAsString;
			public Bitmap responseAsImage;
			public Exception error;
			
			public void getHeaders(HttpURLConnection connection)
			{
				Map<String, List<String>> headers = connection.getHeaderFields();
				
				int size = headers.size();				
				if(size == 0)
				{
					return;
				}
				
				for (int i = 0; i < size - 1; i++) 
				{
					String key = connection.getHeaderFieldKey(i);
					String value = connection.getHeaderField(key);
					this.headers.add(new KeyValuePair(key, value));
				}
			}
			
			public void readResponseStream(HttpURLConnection connection, Stack<Closeable> openedStreams) throws IOException
			{
				InputStream inStream = connection.getInputStream();
				openedStreams.push(inStream);
				
				BufferedInputStream buffer = new java.io.BufferedInputStream(inStream);
				openedStreams.push(buffer);
				
				ByteArrayOutputStream responseStream = new java.io.ByteArrayOutputStream();
				openedStreams.push(responseStream);
				
				int readByte = -1;
				while ((readByte = buffer.read()) != -1)
				{
					responseStream.write(readByte);
				}
				
				this.statusCode = connection.getResponseCode();
				this.raw = responseStream;
				
				// make the byte array conversion here, not in the JavaScript world for better performance
				// since we do not have some explicit way to determine whether the content-type is image,
				// we will do it the dumb way :)
				try
				{
					byte[] data = this.raw.toByteArray();
					this.responseAsImage = BitmapFactory.decodeByteArray(data, 0, data.length);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				
				if(this.responseAsImage == null)
				{
					// convert to string
					this.responseAsString = this.raw.toString();
				}
			}
		}
		
		// TODO: lower-case method
		public static void MakeRequest(RequestOptions options, CompleteCallback callback, Object context)
		{
			new HttpRequestTask(callback, context).execute(options);
		}
		
		static class HttpRequestTask extends AsyncTask<RequestOptions, Void, RequestResult>
		{
			private CompleteCallback callback;
			private Object context;
			
			public HttpRequestTask(CompleteCallback callback, Object context)
			{
				this.callback = callback;
				this.context = context;
			}
			
			@Override
			protected RequestResult doInBackground(RequestOptions... params)
			{
				RequestResult result = new RequestResult();
				Stack<Closeable> openedStreams = new Stack<Closeable>();
				
				try
				{
					RequestOptions options = params[0];
					URL url = new URL(options.url);
					HttpURLConnection connection = (HttpURLConnection)url.openConnection();
					
					// set the request method
					String requestMethod = options.method != null ? options.method.toUpperCase() : "GET";
					connection.setRequestMethod(requestMethod);
					
					// add the headers
					options.addHeaders(connection);
					
					// apply timeout
					if(options.timeout > 0)
					{
						connection.setConnectTimeout(options.timeout);
					}
					
					options.writeContent(connection, openedStreams);
					
					// close the opened streams (saves copy-paste implementation in each method that throws IOException)
					this.closeOpenedStreams(openedStreams);
					
					connection.connect();
					
					// build the result
					result.getHeaders(connection);
					result.readResponseStream(connection, openedStreams);
					
					// close the opened streams (saves copy-paste implementation in each method that throws IOException)
					this.closeOpenedStreams(openedStreams);
					
					connection.disconnect();
					
					return result;
				} 
				catch (Exception e) // TODO: Catch all exceptions? 
				{
					e.printStackTrace();
					result.error = e;
					
					return result;
				}
				finally
				{
					try
					{
						this.closeOpenedStreams(openedStreams);
					}
					catch (IOException e)
					{
						e.printStackTrace();
						// TODO: Java rules - what to do here???
					}
				}
			}
			
			protected void onPostExecute(final RequestResult result) 
			{
				this.callback.onComplete(result, this.context);
		    }
			
			private void closeOpenedStreams(Stack<Closeable> streams) throws IOException
			{
				while(streams.size() > 0)
				{
					Closeable stream = streams.pop();
					stream.close();
				}
			}
		}
	}
	
	static class ImageDownloadTask extends AsyncTask<String, Void, Bitmap> {
		private CompleteCallback callback;
		private Object context;
		
		public ImageDownloadTask(CompleteCallback callback, Object context){
			this.callback = callback;
			this.context = context;
		}
		
		protected Bitmap doInBackground(String... params) {
			InputStream stream = null;
			try {
				stream = new java.net.URL(params[0]).openStream();
				Bitmap bmp = BitmapFactory.decodeStream(stream);
				return bmp;
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			} finally {
				if(stream != null){
					// TODO: WTF??? Try in finally - JAVA Rules!
					try
					{
						stream.close();
					}
					catch (IOException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		
		protected void onPostExecute(final Bitmap result) {
			this.callback.onComplete(result, this.context);
	    }
	}
	
	
}
