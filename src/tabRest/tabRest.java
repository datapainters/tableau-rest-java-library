package tabRest;

////////////////////////////////
// Tableau REST API Library
// version: 1.0.1 BETA
// author: wes@schiesz.com
////////////////////////////////

import java.net.*;
import java.nio.file.Files;
import java.util.Hashtable;
import java.util.Objects;
import java.io.*;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.activation.MimetypesFileTypeMap;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

 
public class tabRest {

	private String ts_url = "";
	private String ts_user = "";
	private String ts_pass = "";
	private String ts_site = "";
	private String ts_token = "";
	private String ts_site_id = "";
	private String ts_user_id = "";
	
	public tabRest (String t_url, String t_user, String t_pass, String t_site) {
		this.ts_url = t_url;
		this.ts_user = t_user;
		this.ts_pass = t_pass;
		this.ts_site = t_site;
	}

	
	////////////////////////
	// WORKBOOKS
	////////////////////////

	// tsPublishWorkbook 
	public String tsPublishWorkbook( String workbook_location, String workbook_name, String project_name) throws Exception {
		String[] full_token = new String[3];
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String tableau_user_name = "";
		String tableau_user_id = "";
		String writer_res = "";
		String xml_line = "";
		
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// call this function from your app code
		//String workbook_id = tsGetWorkbookIdByName(workbook_name);

		String project_id = tsGetProjectIdByName(project_name);
		
		// find out what type of file this is
		String workbook_type = "";
		int responseCode = 0;
		int i = workbook_location.lastIndexOf('.');
		if (i > 0) {
			workbook_type = workbook_location.substring(i+1);
		}
		
		// get the file size
		File wbFile = new File(workbook_location);
		long wbSize = wbFile.length();

		if(wbSize > 60000000)
		{
			// we need to chunk the file
			
		}
		else
		{
			// we can just post it directly			
			String charset = "UTF-8";
			String param = "value";
			
			String boundary = Long.toHexString(System.currentTimeMillis()); // Just generate some unique random value.
			String CRLF = "\r\n"; // Line separator required by multipart/form-data.

			URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/workbooks?overwrite=true");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	        connection.setRequestMethod("POST");  
	        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);	        
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", "multipart/mixed; boundary=" + boundary);

			try (
			    OutputStream output = connection.getOutputStream();
			    PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, charset), true);
			) {
			    // Send normal param.
			    writer.append("--" + boundary).append(CRLF);
			    writer.append("Content-Disposition: name=\"request_payload\"").append(CRLF);
			    writer.append("Content-Type: text/xml;").append(CRLF);
			    writer.append(CRLF).append("<tsRequest><workbook name = \"" + workbook_name + "\" showTabs = \"true\"><project id = \"" + project_id + "\" /></workbook></tsRequest>").append(CRLF).flush();

			    // Send text file.
			    writer.append("--" + boundary).append(CRLF);
			    writer.append("Content-Disposition: name=\"tableau_workbook\"; filename=\"" + wbFile.getName() + "\"").append(CRLF);
			    writer.append("Content-Type: application/octet-stream;").append(CRLF); // Text file itself must be saved in this charset!
			    writer.append(CRLF).flush();
			    Files.copy(wbFile.toPath(), output);
			    output.flush(); // Important before continuing with writer!
			    writer.append(CRLF).flush(); // CRLF is important! It indicates end of boundary.

			    // End of multipart/form-data.
			    writer.append("--" + boundary + "--").flush();
			    
			}

			// Request is lazily fired whenever you need to obtain information about response.
			responseCode = ((HttpURLConnection) connection).getResponseCode();
	        // parse and get the ticket
			/*
	        BufferedReader in = new BufferedReader(
	        						new InputStreamReader(
	        								connection.getInputStream()));
	        String my_line;
	        while ((my_line = in.readLine()) != null) {
	        	xml_line = xml_line + my_line;        	
	        }
	        in.close(); 
			*/
		}
		
		
        tableau_ticket = String.valueOf(responseCode);
		
        // return the ticket
        return tableau_ticket;
	}
	
	// tsDownloadWorkbook 
	public String tsDownloadWorkbook(String workbook_name, String save_path) throws Exception {
		String[] full_token = new String[3];
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String tableau_user_name = "";
		String tableau_user_id = "";
		
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// call this function from your app code
		String workbook_id = tsGetWorkbookIdByName(workbook_name);

		 String fileName = save_path; //The file that will be saved on your computer
		 URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/workbooks/"+workbook_id+"/content"); //The file that you want to download
		URLConnection connection = url.openConnection();
		connection.setRequestProperty("Content-Type", "text/xml");
		connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
		connection.setDoOutput(true);
		 
		 //Code to download
		 InputStream in = new BufferedInputStream(connection.getInputStream());
		 ByteArrayOutputStream out = new ByteArrayOutputStream();
		 byte[] buf = new byte[1024];
		 int n = 0;
		 while (-1!=(n=in.read(buf)))
		 {
		    out.write(buf, 0, n);
		 }
		 out.close();
		 in.close();
		 byte[] response = out.toByteArray();

		 FileOutputStream fos = new FileOutputStream(fileName);
		 fos.write(response);
		 fos.close();
		 //End download code        
        
        
        // return the ticket
        return tableau_ticket;
	}

	// tsGetWorkbooksByUser
	public String[] tsGetWorkbooksByUser(String user_name) throws Exception {
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String tableau_user_name = "";
		String tableau_user_id = "";
		String[] full_token = new String[3]; 
		String xml_line = "";
		String[] user_full = new String[20];
		
		// called the function here
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// get the user id
		tableau_user_id = tsGetUserIdByName(user_name);
		
		// now we post the request for the users on a site
        // now send the new header request
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/users/"+tableau_user_id+"/workbooks");
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));
        String my_line;
        while ((my_line = in.readLine()) != null) {
        	xml_line = xml_line + my_line;        	
        }
        in.close();    		
		
        //System.out.println(xml_line);
        
        // now lets parse the xml and get the response
        DocumentBuilderFactory xparse = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        
        try {           	
        	db = xparse.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml_line));
            try {
                Document xdoc = db.parse(is);
                xdoc.getDocumentElement().normalize();
                
                NodeList nList = xdoc.getElementsByTagName("workbook");
                int user_count = nList.getLength();
                int mycnt=0;
                
                while(mycnt < (user_count))
                {
                	String workbook_id = "";
                	String workbook_name = "";
                	String workbook_contenturl = "";
                	String workbook_showtabs = "";
                	
                    Node nNode = nList.item(mycnt);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			workbook_id = eElement.getAttribute("id");
            			workbook_name = eElement.getAttribute("name");
            			workbook_contenturl = eElement.getAttribute("contentUrl");
            			workbook_showtabs = eElement.getAttribute("showTabs");

                        //System.out.println(mycnt + " - " + user_name);

            			user_full[mycnt] = workbook_id + "," + workbook_name  + "," + workbook_contenturl  + "," + workbook_showtabs;
                    }                    
                	mycnt++;
                }
                
            } catch (SAXException e) {
                // handle SAXException
            } catch (IOException e) {
                // handle IOException
            }
        } catch (ParserConfigurationException e1) {
            // handle ParserConfigurationException
        	System.out.println(e1);
        }        
        
		return user_full;			
	}

	// tsGetWorkbooksByUser
	public String tsGetWorkbookIdByName(String workbook_name) throws Exception {
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String tableau_user_name = "";
		String tableau_user_id = "";
		String my_id = "";
		String[] full_token = new String[3]; 
		String xml_line = "";
		String[] user_full = new String[20];
		
		// called the function here
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// now we post the request for the users on a site
        // now send the new header request
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/users/"+tableau_user+"/workbooks");
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));
        String my_line;
        while ((my_line = in.readLine()) != null) {
        	xml_line = xml_line + my_line;        	
        }
        in.close();    		
		        
        // now lets parse the xml and get the response
        DocumentBuilderFactory xparse = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        
        try {           	
        	db = xparse.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml_line));
            try {
                Document xdoc = db.parse(is);
                xdoc.getDocumentElement().normalize();
                
                NodeList nList = xdoc.getElementsByTagName("workbook");
                int user_count = nList.getLength();
                int mycnt=0;
                
                while(mycnt < (user_count))
                {
                	String workbook_id = "";
                	String wb_name = "";
                	String workbook_contenturl = "";
                	String workbook_showtabs = "";
                	
                    Node nNode = nList.item(mycnt);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			workbook_id = eElement.getAttribute("id");
            			wb_name = eElement.getAttribute("name");
            			workbook_contenturl = eElement.getAttribute("contentUrl");
            			workbook_showtabs = eElement.getAttribute("showTabs");

            			//System.out.println(wb_name + "==" + workbook_name);
            			
            			if(wb_name.equals(workbook_name))
            			{
            				my_id = workbook_id;
            			}
                    }                    
                	mycnt++;
                }
                
            } catch (SAXException e) {
                // handle SAXException
            } catch (IOException e) {
                // handle IOException
            }
        } catch (ParserConfigurationException e1) {
            // handle ParserConfigurationException
        	System.out.println(e1);
        }        
        
		return my_id;			
	}

	// tsGetWorkbooksByUser
	public String tsGetWorkbookNameById(String workbook_id) throws Exception {
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String tableau_user_name = "";
		String tableau_user_id = "";
		String my_id = "";
		String[] full_token = new String[3]; 
		String xml_line = "";
		String[] user_full = new String[20];
		
		// called the function here
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// now we post the request for the users on a site
        // now send the new header request
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/users/"+tableau_user+"/workbooks");
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));
        String my_line;
        while ((my_line = in.readLine()) != null) {
        	xml_line = xml_line + my_line;        	
        }
        in.close();    		
		
        //System.out.println(xml_line);
        
        // now lets parse the xml and get the response
        DocumentBuilderFactory xparse = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        
        try {           	
        	db = xparse.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml_line));
            try {
                Document xdoc = db.parse(is);
                xdoc.getDocumentElement().normalize();
                
                NodeList nList = xdoc.getElementsByTagName("workbook");
                int user_count = nList.getLength();
                int mycnt=0;
                
                while(mycnt < (user_count))
                {
                	String wb_id = "";
                	String wb_name = "";
                	String workbook_contenturl = "";
                	String workbook_showtabs = "";
                	
                    Node nNode = nList.item(mycnt);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			wb_id = eElement.getAttribute("id");
            			wb_name = eElement.getAttribute("name");
            			workbook_contenturl = eElement.getAttribute("contentUrl");
            			workbook_showtabs = eElement.getAttribute("showTabs");

            			if(wb_id.equals(workbook_id))
            			{
            				my_id = wb_name;
            			}
                    }                    
                	mycnt++;
                }
                
            } catch (SAXException e) {
                // handle SAXException
            } catch (IOException e) {
                // handle IOException
            }
        } catch (ParserConfigurationException e1) {
            // handle ParserConfigurationException
        	System.out.println(e1);
        }        
        
		return my_id;			
	}
	
	// tsGetWorkbookById
	public String[] tsGetWorkbookById(String workbook_id) throws Exception {
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String tableau_user_name = "";
		String tableau_user_id = "";
		String[] full_token = new String[3]; 
		String xml_line = "";
		String[] user_full = new String[20];
		
		// called the function here
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// now we post the request for the users on a site
        // now send the new header request
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/workbooks/"+workbook_id+"/");
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));
        String my_line;
        while ((my_line = in.readLine()) != null) {
        	xml_line = xml_line + my_line;        	
        }
        in.close();    		
		
        //System.out.println(xml_line);
        
        // now lets parse the xml and get the response
        DocumentBuilderFactory xparse = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        
        try {           	
        	db = xparse.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml_line));
            try {
                Document xdoc = db.parse(is);
                xdoc.getDocumentElement().normalize();
                
                NodeList nList = xdoc.getElementsByTagName("workbook");
                int user_count = nList.getLength();
                int mycnt=0;
                
                while(mycnt < (user_count))
                {
                	String id = "";
                	String workbook_name = "";
                	String workbook_contenturl = "";
                	String workbook_showtabs = "";
                	
                    Node nNode = nList.item(mycnt);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			id = eElement.getAttribute("id");
            			workbook_name = eElement.getAttribute("name");
            			workbook_contenturl = eElement.getAttribute("contentUrl");
            			workbook_showtabs = eElement.getAttribute("showTabs");

                        //System.out.println(mycnt + " - " + user_name);

            			user_full[mycnt] = id + "," + workbook_name  + "," + workbook_contenturl  + "," + workbook_showtabs;
                    }                    
                	mycnt++;
                }
                
            } catch (SAXException e) {
                // handle SAXException
            } catch (IOException e) {
                // handle IOException
            }
        } catch (ParserConfigurationException e1) {
            // handle ParserConfigurationException
        	System.out.println(e1);
        }        
        
		return user_full;			
	}
	
	// tsGetWorkbookTags
	public String[] tsGetWorkbookTags(String workbook_id) throws Exception {
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String tableau_user_name = "";
		String tableau_user_id = "";
		String[] full_token = new String[3]; 
		String xml_line = "";
		String[] user_full = new String[20];
		
		// called the function here
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// now we post the request for the users on a site
        // now send the new header request
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/workbooks/"+workbook_id+"/");
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));
        String my_line;
        while ((my_line = in.readLine()) != null) {
        	xml_line = xml_line + my_line;        	
        }
        in.close();    		
		
        //System.out.println(xml_line);
        
        // now lets parse the xml and get the response
        DocumentBuilderFactory xparse = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        
        try {           	
        	db = xparse.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml_line));
            try {
                Document xdoc = db.parse(is);
                xdoc.getDocumentElement().normalize();
                
                NodeList nList = xdoc.getElementsByTagName("tag");
                int user_count = nList.getLength();
                int mycnt=0;
                
                while(mycnt < (user_count))
                {
                	String id = "";
                	String workbook_name = "";
                	String workbook_contenturl = "";
                	String workbook_showtabs = "";
                	
                    Node nNode = nList.item(mycnt);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			workbook_name = eElement.getAttribute("label");

            			user_full[mycnt] = workbook_name;
                    }                    
                	mycnt++;
                }
                
            } catch (SAXException e) {
                // handle SAXException
            } catch (IOException e) {
                // handle IOException
            }
        } catch (ParserConfigurationException e1) {
            // handle ParserConfigurationException
        	System.out.println(e1);
        }        
        
		return user_full;			
	}
		
	// tsGetViewsFromWorkbook
	public String[] tsGetViewsFromWorkbook(String workbook_id) throws Exception {
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String tableau_user_name = "";
		String tableau_user_id = "";
		String[] full_token = new String[3]; 
		String xml_line = "";
		String[] user_full = new String[20];
		
		// called the function here
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// get the user id
//		tableau_user_id = getUserId(user_name, ts_url, ts_user, ts_pass);
		
		// now we post the request for the users on a site
        // now send the new header request
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/workbooks/"+workbook_id+"/views");
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));
        String my_line;
        while ((my_line = in.readLine()) != null) {
        	xml_line = xml_line + my_line;        	
        }
        in.close();    		
		
        //System.out.println(xml_line);
        
        // now lets parse the xml and get the response
        DocumentBuilderFactory xparse = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        
        try {           	
        	db = xparse.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml_line));
            try {
                Document xdoc = db.parse(is);
                xdoc.getDocumentElement().normalize();
                
                NodeList nList = xdoc.getElementsByTagName("view");
                int user_count = nList.getLength();
                int mycnt=0;
                
                while(mycnt < (user_count))
                {
                	String view_id = "";
                	String workbook_name = "";
                	String workbook_contenturl = "";
                	String workbook_showtabs = "";
                	
                    Node nNode = nList.item(mycnt);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			view_id = eElement.getAttribute("id");
            			workbook_name = eElement.getAttribute("name");
            			workbook_contenturl = eElement.getAttribute("contentUrl");

                        //System.out.println(mycnt + " - " + user_name);

            			user_full[mycnt] = view_id + "," + workbook_name  + "," + workbook_contenturl;
                    }                    
                	mycnt++;
                }
                
            } catch (SAXException e) {
                // handle SAXException
            } catch (IOException e) {
                // handle IOException
            }
        } catch (ParserConfigurationException e1) {
            // handle ParserConfigurationException
        	System.out.println(e1);
        }        
        
		return user_full;					
	}

	// tsGetViewIdByName
	public String tsGetViewIdByName(String workbook_id, String view_name) throws Exception {
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String tableau_user_name = "";
		String tableau_user_id = "";
		String[] full_token = new String[3]; 
		String xml_line = "";
		String[] user_full = new String[20];
		String response = "";
		
		// called the function here
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// get the user id
//		tableau_user_id = getUserId(user_name, ts_url, ts_user, ts_pass);
		
		// now we post the request for the users on a site
        // now send the new header request
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/workbooks/"+workbook_id+"/views");
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));
        String my_line;
        while ((my_line = in.readLine()) != null) {
        	xml_line = xml_line + my_line;        	
        }
        in.close();    		
		
        //System.out.println(xml_line);
        
        // now lets parse the xml and get the response
        DocumentBuilderFactory xparse = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        
        try {           	
        	db = xparse.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml_line));
            try {
                Document xdoc = db.parse(is);
                xdoc.getDocumentElement().normalize();
                
                NodeList nList = xdoc.getElementsByTagName("view");
                int user_count = nList.getLength();
                int mycnt=0;
                
                while(mycnt < (user_count))
                {
                	String view_id = "";
                	String workbook_name = "";
                	String workbook_contenturl = "";
                	String workbook_showtabs = "";
                	
                    Node nNode = nList.item(mycnt);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			view_id = eElement.getAttribute("id");
            			workbook_name = eElement.getAttribute("name");
            			workbook_contenturl = eElement.getAttribute("contentUrl");

                        //System.out.println(mycnt + " - " + user_name);
            			if(workbook_name.equals(view_name))
            			{
            				response = view_id;
            			}
                    }                    
                	mycnt++;
                }
                
            } catch (SAXException e) {
                // handle SAXException
            } catch (IOException e) {
                // handle IOException
            }
        } catch (ParserConfigurationException e1) {
            // handle ParserConfigurationException
        	System.out.println(e1);
        }        
        
		return response;					
	}

	// tsGetViewNameById
	public String tsGetViewNameById(String workbook_id, String view_id) throws Exception {
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String tableau_user_name = "";
		String tableau_user_id = "";
		String[] full_token = new String[3]; 
		String xml_line = "";
		String[] user_full = new String[20];
		String response = "";
		
		// called the function here
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// now we post the request for the users on a site
        // now send the new header request
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/workbooks/"+workbook_id+"/views");
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));
        String my_line;
        while ((my_line = in.readLine()) != null) {
        	xml_line = xml_line + my_line;        	
        }
        in.close();    		
		
        //System.out.println(xml_line);
        
        // now lets parse the xml and get the response
        DocumentBuilderFactory xparse = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        
        try {           	
        	db = xparse.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml_line));
            try {
                Document xdoc = db.parse(is);
                xdoc.getDocumentElement().normalize();
                
                NodeList nList = xdoc.getElementsByTagName("view");
                int user_count = nList.getLength();
                int mycnt=0;
                
                while(mycnt < (user_count))
                {
                	String mview_id = "";
                	String workbook_name = "";
                	String workbook_contenturl = "";
                	String workbook_showtabs = "";
                	
                    Node nNode = nList.item(mycnt);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			mview_id = eElement.getAttribute("id");
            			workbook_name = eElement.getAttribute("name");
            			workbook_contenturl = eElement.getAttribute("contentUrl");

                        //System.out.println(mycnt + " - " + user_name);
            			if(mview_id.equals(view_id))
            			{
            				response = workbook_name;
            			}
                    }                    
                	mycnt++;
                }
                
            } catch (SAXException e) {
                // handle SAXException
            } catch (IOException e) {
                // handle IOException
            }
        } catch (ParserConfigurationException e1) {
            // handle ParserConfigurationException
        	System.out.println(e1);
        }        
        
		return response;					
	}

	// tsGetWorkbookConnections
	public String[] tsGetWorkbookConnections(String workbook_id) throws Exception {
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String tableau_user_name = "";
		String tableau_user_id = "";
		String[] full_token = new String[3]; 
		String xml_line = "";
		String[] user_full = new String[20];
		
		// called the function here
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// get the user id
//		tableau_user_id = getUserId(user_name, ts_url, ts_user, ts_pass);
		
		// now we post the request for the users on a site
        // now send the new header request
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/workbooks/"+workbook_id+"/connections");
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));
        String my_line;
        while ((my_line = in.readLine()) != null) {
        	xml_line = xml_line + my_line;        	
        }
        in.close();    		
		
        //System.out.println(xml_line);
        
        // now lets parse the xml and get the response
        DocumentBuilderFactory xparse = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        
        try {           	
        	db = xparse.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml_line));
            try {
                Document xdoc = db.parse(is);
                xdoc.getDocumentElement().normalize();
                
                NodeList nList = xdoc.getElementsByTagName("connection");
                int user_count = nList.getLength();
                int mycnt=0;
                
                while(mycnt < (user_count))
                {
                	String connect_id = "";
                	String c_type = "";
                	
                    Node nNode = nList.item(mycnt);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			connect_id = eElement.getAttribute("id");
            			c_type = eElement.getAttribute("type");

                        //System.out.println(mycnt + " - " + user_name);

            			user_full[mycnt] = connect_id + "," + c_type;
                    }                    
                	mycnt++;
                }
                
            } catch (SAXException e) {
                // handle SAXException
            } catch (IOException e) {
                // handle IOException
            }
        } catch (ParserConfigurationException e1) {
            // handle ParserConfigurationException
        	System.out.println(e1);
        }        
        
		return user_full;					
	}

	// getWorkbookIcon
	public InputStream tsGetWorkbookIcon(String workbook_id) throws Exception {
		String tableau_ticket = "";
		String tableau_site = "";
		String[] full_token = new String[3]; 
		String xml_line = "";
		String[] user_full = new String[20];
		InputStream is = null;
		
		// called the function here
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		
		// now we post the request for the users on a site
        // now send the new header request
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/workbooks/"+workbook_id+"/previewImage");
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        // here we need to get the input stream and return it        
        is = connection.getInputStream();
        
		return is;				
	}
	
	// tsGetWorkbookViewIcon
	public InputStream tsGetWorkbookViewIcon(String workbook_id, String view_id) throws Exception {
		String tableau_ticket = "";
		String tableau_site = "";
		String[] full_token = new String[3]; 
		String xml_line = "";
		String[] user_full = new String[20];
		InputStream is = null;
		
		// called the function here
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		
		// now we post the request for the users on a site
        // now send the new header request
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/workbooks/"+workbook_id+"/views/"+view_id+"/previewImage");
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        is = connection.getInputStream();
        
		return is;	
	}	

	// tsUpdateWorkbook
	public String tsUpdateWorkbook(String workbook_id, String project_id, String show_tabs, String owner_id) throws Exception {
		String[] full_token = new String[3];
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String tableau_user_name = "";
		String tableau_user_id = "";
		String xmlString = "";
		
        //Creating an empty XML Document
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        //Creating the XML tree

        //create the root element and add it to the document
        Element root = doc.createElement("tsRequest");
        doc.appendChild(root);

        //create child element, add an attribute, and add to root
        Element child = doc.createElement("workbook");
        child.setAttribute("showTabs", show_tabs);
        root.appendChild(child);

        // last element
        Element ste = doc.createElement("project");
        ste.setAttribute("id", project_id);
        child.appendChild(ste);            

        Element ste2 = doc.createElement("owner");
        ste2.setAttribute("id", owner_id);
        child.appendChild(ste2);            
        
        //Output the XML to a string
        //set up a transformer
        TransformerFactory transfac = TransformerFactory.newInstance();
        Transformer trans = transfac.newTransformer();
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        trans.setOutputProperty(OutputKeys.INDENT, "yes");

        //create string from xml tree
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        DOMSource source = new DOMSource(doc);
        trans.transform(source, result);
        xmlString = sw.toString();		
		
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// call this function from your app code
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/workbooks/"+workbook_id);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("PUT"); 
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);


        // prepare the postvars
        OutputStreamWriter out = new OutputStreamWriter(
                                         connection.getOutputStream());
        out.write(xmlString);
        out.close();      
        
        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));

        //String tableau_ticket = "";
        String my_line;
        while ((my_line = in.readLine()) != null) {
            tableau_ticket = my_line;        	
        }
        in.close();
        
        // return the ticket
        return tableau_ticket;
	}

	// tsDeleteWorkbook
	public void tsDeleteWorkbook(String workbook_id) throws Exception {
		String[] full_token = new String[3];
		String tableau_ticket = "";
		String tableau_site = "";
		String xmlString = "";
				
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		
		// call this function from your app code
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/workbooks/"+workbook_id);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("DELETE");        
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
//        connection.setDoOutput(true);
        
        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));

	}
	
	// tsUpdateWorkbookConnection
	public String tsUpdateWorkbookConnection(String workbook_id, String connection_id, String serverAddress, String serverPort, String userName, String password) throws Exception {
		String[] full_token = new String[3];
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String tableau_user_name = "";
		String tableau_user_id = "";
		String xmlString = "";
		
        //Creating an empty XML Document
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        //Creating the XML tree

        //create the root element and add it to the document
        Element root = doc.createElement("tsRequest");
        doc.appendChild(root);

        //create child element, add an attribute, and add to root
        Element child = doc.createElement("connection");
        child.setAttribute("serverAddress", serverAddress);
        child.setAttribute("serverPort", serverPort	);
        child.setAttribute("userName", userName);
        child.setAttribute("password", password);
        root.appendChild(child);
        
        //Output the XML to a string
        //set up a transformer
        TransformerFactory transfac = TransformerFactory.newInstance();
        Transformer trans = transfac.newTransformer();
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        trans.setOutputProperty(OutputKeys.INDENT, "yes");

        //create string from xml tree
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        DOMSource source = new DOMSource(doc);
        trans.transform(source, result);
        xmlString = sw.toString();		
		
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// call this function from your app code
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/workbooks/"+workbook_id+"/connections/"+connection_id);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("PUT"); 
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);


        // prepare the postvars
        OutputStreamWriter out = new OutputStreamWriter(
                                         connection.getOutputStream());
        out.write(xmlString);
        out.close();      
        
        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));

        //String tableau_ticket = "";
        String my_line;
        while ((my_line = in.readLine()) != null) {
            tableau_ticket = my_line;        	
        }
        in.close();
        
        // return the ticket
        return tableau_ticket;
	}

	// tsAddWorkbookPermission
	public String tsAddWorkbookPermission(String workbook_id, String user_id, String group_id, String capability_name, String capability_mode) throws Exception {
		String[] full_token = new String[3];
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String tableau_user_name = "";
		String tableau_user_id = "";
		String xmlString = "";
		
        //Creating an empty XML Document
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        //Creating the XML tree

        //create the root element and add it to the document
        Element root = doc.createElement("tsRequest");
        doc.appendChild(root);

        //create child element, add an attribute, and add to root
        Element child = doc.createElement("permissions");
        root.appendChild(child);

        // last element
        Element ste = doc.createElement("workbook");
        ste.setAttribute("id", workbook_id);
        child.appendChild(ste);            

        Element ste2 = doc.createElement("granteeCapabilities");
        child.appendChild(ste2);            

        if(group_id.length() > 0)
        {
            Element ste3 = doc.createElement("group");
            ste3.setAttribute("id", group_id);
            ste2.appendChild(ste3);                   	
        }

        if(user_id.length() > 0)
        {
            Element ste3 = doc.createElement("user");
            ste3.setAttribute("id", user_id);
            ste2.appendChild(ste3);                   	
        }

        Element ste3 = doc.createElement("capabilities");
        ste2.appendChild(ste3);            
        
        Element ste4 = doc.createElement("capability");
        ste4.setAttribute("name", capability_name);
        ste4.setAttribute("mode", capability_mode);
        ste3.appendChild(ste4);            

        
        //Output the XML to a string
        //set up a transformer
        TransformerFactory transfac = TransformerFactory.newInstance();
        Transformer trans = transfac.newTransformer();
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        trans.setOutputProperty(OutputKeys.INDENT, "yes");

        //create string from xml tree
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        DOMSource source = new DOMSource(doc);
        trans.transform(source, result);
        xmlString = sw.toString();		
		
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// call this function from your app code
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/workbooks/"+workbook_id+"/permissions/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("PUT");  
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);


        // prepare the postvars
        OutputStreamWriter out = new OutputStreamWriter(
                                         connection.getOutputStream());
        out.write(xmlString);
        out.close();      
        
        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));

        //String tableau_ticket = "";
        String my_line;
        while ((my_line = in.readLine()) != null) {
            tableau_ticket = my_line;        	
        }
        in.close();
        
        // return the ticket
        return tableau_ticket;
	}

	// tsDeleteWorkbookPermission
	public void tsDeleteWorkbookPermission(String workbook_id, String user_id, String group_id, String capability_name, String capability_mode) throws Exception {
		String[] full_token = new String[3];
		String tableau_ticket = "";
		String tableau_site = "";
		String xmlString = "";
				
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		URL url;
		
		// call this function from your app code
		if(user_id.length() > 0)
		{
			url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/workbooks/"+workbook_id + "/permissions/" + "users/" + user_id +"/"+ capability_name + "/" + capability_mode);
		}
		else
		{
			url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/workbooks/"+workbook_id + "/permissions/" + "groups/" + group_id +"/"+ capability_name + "/" + capability_mode);			
		}
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("DELETE");        
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
//        connection.setDoOutput(true);
        
        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));

	}

	// tsGetPermissionsFromWorkbook
	public String[] tsGetPermissionsFromWorkbook(String workbook_id) throws Exception {
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String tableau_user_name = "";
		String tableau_user_id = "";
		String[] full_token = new String[3]; 
		String xml_line = "";
		String[] user_full = new String[200];
		
		// called the function here
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// get the user id
//		tableau_user_id = getUserId(user_name, ts_url, ts_user, ts_pass);
		
		// now we post the request for the users on a site
        // now send the new header request
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/workbooks/"+workbook_id+"/permissions");
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));
        String my_line;
        while ((my_line = in.readLine()) != null) {
        	xml_line = xml_line + my_line;        	
        }
        in.close();    		
		
        //System.out.println(xml_line);
        
        // now lets parse the xml and get the response
        DocumentBuilderFactory xparse = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        
        try {           	
        	db = xparse.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml_line));
            try {
                Document xdoc = db.parse(is);
                xdoc.getDocumentElement().normalize();
            	String user_id = "";
            	String group_id = "";
            	String wb_id = "";
            	String wb_name = "";
            	String cap_name = "";
            	String cap_mode = "";
                
                NodeList nList = xdoc.getElementsByTagName("workbook");
                int user_count = nList.getLength();
                int mycnt=0;
                int mycnt5=0;
                
                while(mycnt < (user_count))
                {                	
                    Node nNode = nList.item(mycnt);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			String c_name = eElement.getTagName();
            			
            			if(c_name == "workbook")
            			{
            				wb_id = eElement.getAttribute("id");
            				wb_name = eElement.getAttribute("name");
            			}

                    }                    
                	mycnt++;
                }
                
                NodeList nList2 = xdoc.getElementsByTagName("user");
                int user_count2 = nList2.getLength();
                int mycnt2=0;
                
                while(mycnt2 < (user_count2))
                {
                    Node nNode = nList2.item(mycnt2);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			String c_name = eElement.getTagName();
            			
            			if(c_name == "user")
            			{
            				user_id = eElement.getAttribute("id");
            			}
                    }                    
                	mycnt2++;
                }
                //
                NodeList nList3 = xdoc.getElementsByTagName("group");
                int user_count3 = nList3.getLength();
                int mycnt3=0;
                
                while(mycnt3 < (user_count3))
                {
                    Node nNode = nList3.item(mycnt3);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			String c_name = eElement.getTagName();
            			
            			if(c_name == "group")
            			{
            				group_id = eElement.getAttribute("id");
            			}
                    }                    
                	mycnt3++;
                }

                // 
                NodeList nList4 = xdoc.getElementsByTagName("capability");
                int user_count4 = nList4.getLength();
                int mycnt4=0;
                
                while(mycnt4 < (user_count4))
                {
                    Node nNode = nList4.item(mycnt4);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			String c_name = eElement.getTagName();
            			
            			if(c_name == "capability")
            			{
            				cap_name = eElement.getAttribute("name");
            				cap_mode = eElement.getAttribute("mode");
                			user_full[mycnt5] = wb_id + "," + wb_name + "," + user_id + "," + group_id + "," + cap_name + "," + cap_mode;
                        	mycnt5++;
            			}
                    }                    
                	mycnt4++;
                }

                } catch (SAXException e) {
                // handle SAXException
            } catch (IOException e) {
                // handle IOException
            }
        } catch (ParserConfigurationException e1) {
            // handle ParserConfigurationException
        	System.out.println(e1);
        }        
        
		return user_full;					
	}
	
	// tsAddWorkbookPermission
	public String tsAddWorkbookTag(String workbook_id, String tag_name) throws Exception {
		String[] full_token = new String[3];
		String tableau_ticket = "";
		String tableau_site = "";
		String xmlString = "";

		// fix the name
		tag_name.replaceAll("\\s","");
		
        //Creating an empty XML Document
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        //Creating the XML tree

        //create the root element and add it to the document
        Element root = doc.createElement("tsRequest");
        doc.appendChild(root);

        //create child element, add an attribute, and add to root
        Element child = doc.createElement("tags");
        root.appendChild(child);

        // last element
        Element ste = doc.createElement("tag");
        ste.setAttribute("label", tag_name);
        child.appendChild(ste);            
        
        //Output the XML to a string
        //set up a transformer
        TransformerFactory transfac = TransformerFactory.newInstance();
        Transformer trans = transfac.newTransformer();
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        trans.setOutputProperty(OutputKeys.INDENT, "yes");

        //create string from xml tree
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        DOMSource source = new DOMSource(doc);
        trans.transform(source, result);
        xmlString = sw.toString();		
		
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		
		// call this function from your app code
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/workbooks/"+workbook_id+"/tags/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("PUT");  
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        // prepare the postvars
        OutputStreamWriter out = new OutputStreamWriter(
                                         connection.getOutputStream());
        out.write(xmlString);
        out.close();      
        
        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));

        //String tableau_ticket = "";
        String my_line;
        while ((my_line = in.readLine()) != null) {
            tableau_ticket = my_line;        	
        }
        in.close();
        
        // return the ticket
        return tableau_ticket;
	}
	
	// tsDeleteWorkbookTag
	public void tsDeleteWorkbookTag(String workbook_id, String tag_name) throws Exception {
		String[] full_token = new String[3];
		String tableau_ticket = "";
		String tableau_site = "";
		String xmlString = "";
				
		// fix the name
		tag_name.replaceAll("\\s","");		
		
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		
		// call this function from your app code
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/workbooks/"+workbook_id+"/tags/"+tag_name);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("DELETE");        
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
//        connection.setDoOutput(true);
        
        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));

	}

	// tsAddWorkbookPermission
	public String tsAddWorkbookFavorite(String workbook_id, String user_id, String favorite_name) throws Exception {
		String[] full_token = new String[3];
		String tableau_ticket = "";
		String tableau_site = "";
		String xmlString = "";
		
        //Creating an empty XML Document
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        //Creating the XML tree

        //create the root element and add it to the document
        Element root = doc.createElement("tsRequest");
        doc.appendChild(root);

        //create child element, add an attribute, and add to root
        Element child = doc.createElement("favorite");
        child.setAttribute("label", favorite_name);
        root.appendChild(child);

        // last element
        Element ste = doc.createElement("workbook");
        ste.setAttribute("id", workbook_id);
        child.appendChild(ste);            
        
        //Output the XML to a string
        //set up a transformer
        TransformerFactory transfac = TransformerFactory.newInstance();
        Transformer trans = transfac.newTransformer();
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        trans.setOutputProperty(OutputKeys.INDENT, "yes");

        //create string from xml tree
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        DOMSource source = new DOMSource(doc);
        trans.transform(source, result);
        xmlString = sw.toString();		
		
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		
		// call this function from your app code
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/favorites/"+user_id);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("PUT");  
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        // prepare the postvars
        OutputStreamWriter out = new OutputStreamWriter(
                                         connection.getOutputStream());
        out.write(xmlString);
        out.close();      
        
        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));

        //String tableau_ticket = "";
        String my_line;
        while ((my_line = in.readLine()) != null) {
            tableau_ticket = my_line;        	
        }
        in.close();
        
        // return the ticket
        return tableau_ticket;
	}

	// tsDeleteWorkbook
	public void tsDeleteWorkbookFavorite(String workbook_id, String user_id) throws Exception {
		String[] full_token = new String[3];
		String tableau_ticket = "";
		String tableau_site = "";
		String xmlString = "";
				
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		
		// call this function from your app code
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/favorites/"+user_id+"/workbooks/"+workbook_id);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("DELETE");        
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
//        connection.setDoOutput(true);
        
        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));

	}
	
	// tsAddWorkbookPermission
	public String tsAddViewFavorite(String view_id, String user_id, String favorite_name) throws Exception {
		String[] full_token = new String[3];
		String tableau_ticket = "";
		String tableau_site = "";
		String xmlString = "";
		
        //Creating an empty XML Document
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        //Creating the XML tree

        //create the root element and add it to the document
        Element root = doc.createElement("tsRequest");
        doc.appendChild(root);

        //create child element, add an attribute, and add to root
        Element child = doc.createElement("favorite");
        child.setAttribute("label", favorite_name);
        root.appendChild(child);

        // last element
        Element ste = doc.createElement("view");
        ste.setAttribute("id", view_id);
        child.appendChild(ste);            
        
        //Output the XML to a string
        //set up a transformer
        TransformerFactory transfac = TransformerFactory.newInstance();
        Transformer trans = transfac.newTransformer();
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        trans.setOutputProperty(OutputKeys.INDENT, "yes");

        //create string from xml tree
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        DOMSource source = new DOMSource(doc);
        trans.transform(source, result);
        xmlString = sw.toString();		
		
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		
		// call this function from your app code
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/favorites/"+user_id);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("PUT");  
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        // prepare the postvars
        OutputStreamWriter out = new OutputStreamWriter(
                                         connection.getOutputStream());
        out.write(xmlString);
        out.close();      
        
        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));

        //String tableau_ticket = "";
        String my_line;
        while ((my_line = in.readLine()) != null) {
            tableau_ticket = my_line;        	
        }
        in.close();
        
        // return the ticket
        return tableau_ticket;
	}

	// tsDeleteWorkbook
	public void tsDeleteViewFavorite(String view_id, String user_id) throws Exception {
		String[] full_token = new String[3];
		String tableau_ticket = "";
		String tableau_site = "";
		String xmlString = "";
				
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		
		// call this function from your app code
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/favorites/"+user_id+"/views/"+view_id);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("DELETE");        
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
//        connection.setDoOutput(true);
        
        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));

	}
	
	
	
	////////////////////////
	// SITES
	////////////////////////

	// tsGetSites
	public String[] tsGetSites() throws Exception {
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String[] full_token = new String[3]; 
		String xml_line = "";
		String[] user_full = new String[20];
		
		// called the function here
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// now we post the request for the users on a site
        // now send the new header request
        URL url = new URL(ts_url+"/api/2.0/sites/");
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));
        String my_line;
        while ((my_line = in.readLine()) != null) {
        	xml_line = xml_line + my_line;        	
        }
        in.close();    		
		
        // now lets parse the xml and get the response
        DocumentBuilderFactory xparse = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        
        try {           	
        	db = xparse.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml_line));
            try {
                Document xdoc = db.parse(is);
                xdoc.getDocumentElement().normalize();
                
                NodeList nList = xdoc.getElementsByTagName("site");
                int user_count = nList.getLength();
                int mycnt=0;
                
                while(mycnt < (user_count))
                {
                	String user_id = "";
                	String user_name = "";
                	String contentUrl = "";
                	String adminMode = "";
                	String userQuota = "";
                	String storageQuota = "";
                	String state = "";
                	String statusReason = "";
                	
                    Node nNode = nList.item(mycnt);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			user_id = eElement.getAttribute("id");
            			user_name = eElement.getAttribute("name");
            			contentUrl = eElement.getAttribute("contentUrl");
            			adminMode = eElement.getAttribute("adminMode");
            			userQuota = eElement.getAttribute("userQuota");
            			storageQuota = eElement.getAttribute("storageQuota");
            			state = eElement.getAttribute("state");
            			statusReason = eElement.getAttribute("statusReason");

            			user_full[mycnt] = user_id + "," + user_name  + "," + contentUrl  + "," + adminMode  + "," + userQuota  + "," + storageQuota  + "," + state  + "," + statusReason;
                    }                    
                	mycnt++;
                }
                
            } catch (SAXException e) {
                // handle SAXException
            } catch (IOException e) {
                // handle IOException
            }
        } catch (ParserConfigurationException e1) {
            // handle ParserConfigurationException
        	System.out.println(e1);
        }        
        
		return user_full;		
	}	

	// tsGetSiteIdByName
	public String tsGetSiteIdByName(String site_name) throws Exception {
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String[] full_token = new String[3]; 
		String xml_line = "";
		String[] user_full = new String[20];
		
		// called the function here
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// now we post the request for the users on a site
        // now send the new header request
        URL url = new URL(ts_url+"/api/2.0/sites/");
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));
        String my_line;
        while ((my_line = in.readLine()) != null) {
        	xml_line = xml_line + my_line;        	
        }
        in.close();    		
		
        // now lets parse the xml and get the response
        DocumentBuilderFactory xparse = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        String user_id = "";
        try {           	
        	db = xparse.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml_line));
            try {
                Document xdoc = db.parse(is);
                xdoc.getDocumentElement().normalize();
                
                NodeList nList = xdoc.getElementsByTagName("site");
                int user_count = nList.getLength();
                int mycnt=0;
                
                while(mycnt < (user_count))
                {
                	
                	String user_name = "";
                	String contentUrl = "";
                	String adminMode = "";
                	String userQuota = "";
                	String storageQuota = "";
                	String state = "";
                	String statusReason = "";
                	
                    Node nNode = nList.item(mycnt);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			
            			if(eElement.getAttribute("name") != null && eElement.getAttribute("name").equals(site_name))
            			{
            				user_id = eElement.getAttribute("id");
            			}
                    }                    
                	mycnt++;
                }
                
            } catch (SAXException e) {
                // handle SAXException
            } catch (IOException e) {
                // handle IOException
            }
        } catch (ParserConfigurationException e1) {
            // handle ParserConfigurationException
        	System.out.println(e1);
        }        
        
		return user_id;		
	}	

	// tsGetSiteContentUrlByName
	public  String tsGetSiteContentUrlByName(String site_name) throws Exception {
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String[] full_token = new String[3]; 
		String xml_line = "";
		String[] user_full = new String[20];
		
		// called the function here
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// now we post the request for the users on a site
        // now send the new header request
        URL url = new URL(ts_url+"/api/2.0/sites/");
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));
        String my_line;
        while ((my_line = in.readLine()) != null) {
        	xml_line = xml_line + my_line;        	
        }
        in.close();    		
		
        // now lets parse the xml and get the response
        DocumentBuilderFactory xparse = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        String user_id = "";
        try {           	
        	db = xparse.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml_line));
            try {
                Document xdoc = db.parse(is);
                xdoc.getDocumentElement().normalize();
                
                NodeList nList = xdoc.getElementsByTagName("site");
                int user_count = nList.getLength();
                int mycnt=0;
                
                while(mycnt < (user_count))
                {
                	
                	String user_name = "";
                	String contentUrl = "";
                	String adminMode = "";
                	String userQuota = "";
                	String storageQuota = "";
                	String state = "";
                	String statusReason = "";
                	
                    Node nNode = nList.item(mycnt);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			
            			if(eElement.getAttribute("name") != null && eElement.getAttribute("name").equals(site_name))
            			{
            				user_id = eElement.getAttribute("contentUrl");
            			}
                    }                    
                	mycnt++;
                }
                
            } catch (SAXException e) {
                // handle SAXException
            } catch (IOException e) {
                // handle IOException
            }
        } catch (ParserConfigurationException e1) {
            // handle ParserConfigurationException
        	System.out.println(e1);
        }        
        
		return user_id;		
	}	
	
	// tsGetSiteNameById
	public String tsGetSiteNameById(String site_id) throws Exception {
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String[] full_token = new String[3]; 
		String xml_line = "";
		String[] user_full = new String[20];
		
		// called the function here
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// now we post the request for the users on a site
        // now send the new header request
        URL url = new URL(ts_url+"/api/2.0/sites/");
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));
        String my_line;
        while ((my_line = in.readLine()) != null) {
        	xml_line = xml_line + my_line;        	
        }
        in.close();    		
		
        // now lets parse the xml and get the response
        DocumentBuilderFactory xparse = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        String user_id = "";
        try {           	
        	db = xparse.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml_line));
            try {
                Document xdoc = db.parse(is);
                xdoc.getDocumentElement().normalize();
                
                NodeList nList = xdoc.getElementsByTagName("site");
                int user_count = nList.getLength();
                int mycnt=0;
                
                while(mycnt < (user_count))
                {
                	
                	String user_name = "";
                	String contentUrl = "";
                	String adminMode = "";
                	String userQuota = "";
                	String storageQuota = "";
                	String state = "";
                	String statusReason = "";
                	
                    Node nNode = nList.item(mycnt);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			
            			if(eElement.getAttribute("id") != null && eElement.getAttribute("id").equals(site_id))
            			{
            				user_id = eElement.getAttribute("name");
            			}
                    }                    
                	mycnt++;
                }
                
            } catch (SAXException e) {
                // handle SAXException
            } catch (IOException e) {
                // handle IOException
            }
        } catch (ParserConfigurationException e1) {
            // handle ParserConfigurationException
        	System.out.println(e1);
        }        
        
		return user_id;		
	}	

	// tsAddSite
	public String tsAddSite(String site_name) throws Exception {
		String[] full_token = new String[3];
		String tableau_ticket = "";
		String tableau_site = "";
		String xmlString = "";
		
        //Creating an empty XML Document
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        //Creating the XML tree

        //create the root element and add it to the document
        Element root = doc.createElement("tsRequest");
        doc.appendChild(root);

        //create child element, add an attribute, and add to root
        Element child = doc.createElement("site");
        child.setAttribute("name", site_name);
        child.setAttribute("contentUrl", site_name.replace(" ", ""));
        root.appendChild(child);

        //Output the XML to a string
        //set up a transformer
        TransformerFactory transfac = TransformerFactory.newInstance();
        Transformer trans = transfac.newTransformer();
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        trans.setOutputProperty(OutputKeys.INDENT, "yes");

        //create string from xml tree
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        DOMSource source = new DOMSource(doc);
        trans.transform(source, result);
        xmlString = sw.toString();		
		
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		
		// call this function from your app code
        URL url = new URL(ts_url+"/api/2.0/sites/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");  
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        // prepare the postvars
        OutputStreamWriter out = new OutputStreamWriter(
                                         connection.getOutputStream());
        out.write(xmlString);
        out.close();      
        
        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));

        //String tableau_ticket = "";
        String my_line;
        tableau_ticket = "";
        while ((my_line = in.readLine()) != null) {
            tableau_ticket = tableau_ticket + my_line;        	
        }
        in.close();
        
        // get the user_id for the new record
        
        // now lets parse the xml and get the response
        DocumentBuilderFactory xparse = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        
        try {           	
        	db = xparse.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(tableau_ticket));
            try {
                Document xdoc = db.parse(is);
                xdoc.getDocumentElement().normalize();
                
                NodeList nList = xdoc.getElementsByTagName("site");
                int user_count = nList.getLength();
                int mycnt=0;
                
                while(mycnt < (user_count))
                {
                    Node nNode = nList.item(mycnt);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			tableau_ticket = eElement.getAttribute("id");
                    }                    
                	mycnt++;
                }
                
            } catch (SAXException e) {
                // handle SAXException
            } catch (IOException e) {
                // handle IOException
            }
        } catch (ParserConfigurationException e1) {
            // handle ParserConfigurationException
        	System.out.println(e1);
        } 
        
        // return the ticket
        return tableau_ticket;
	}
	
	// tsDeleteSite
	public void tsDeleteSite(String site_id) throws Exception {
		String[] full_token = new String[3];
		String tableau_ticket = "";
		String tableau_site = "";
		String xmlString = "";
				
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		
		// call this function from your app code
        URL url = new URL(ts_url+"/api/2.0/sites/"+site_id);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("DELETE");        
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
//        connection.setDoOutput(true);
        
        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));

	}
	
	// tsUpdateSite
	public String tsUpdateSite(String site_id, String site_name, String contentUrl, String adminMode, String userQuota, String state, String storageQuote, String disableSubscriptions) throws Exception {
		String[] full_token = new String[3];
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String tableau_user_name = "";
		String tableau_user_id = "";
		String xmlString = "";
		
        //Creating an empty XML Document
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        //Creating the XML tree

        //create the root element and add it to the document
        Element root = doc.createElement("tsRequest");
        doc.appendChild(root);

        Element child = doc.createElement("site");
        child.setAttribute("name", site_name);
        if(contentUrl.length()>0){
        	child.setAttribute("contentUrl", contentUrl);
        }
        if(adminMode.length()>0){
        	child.setAttribute("adminMode", adminMode);
        }
        if(userQuota.length()>0){
        	child.setAttribute("userQuota", userQuota);
        }
        if(state.length()>0){
        	child.setAttribute("state", state);
        }
        if(storageQuote.length()>0){
        	child.setAttribute("storageQuote", storageQuote);
        }
        if(disableSubscriptions.length()>0){
        	child.setAttribute("disableSubscriptions", disableSubscriptions);
        }
        root.appendChild(child);
        
        //Output the XML to a string
        //set up a transformer
        TransformerFactory transfac = TransformerFactory.newInstance();
        Transformer trans = transfac.newTransformer();
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        trans.setOutputProperty(OutputKeys.INDENT, "yes");

        //create string from xml tree
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        DOMSource source = new DOMSource(doc);
        trans.transform(source, result);
        xmlString = sw.toString();		
		
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// call this function from your app code
        URL url = new URL(ts_url+"/api/2.0/sites/"+site_id);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("PUT");          
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);


        // prepare the postvars
        OutputStreamWriter out = new OutputStreamWriter(
                                         connection.getOutputStream());
        out.write(xmlString);
        out.close();      
        
        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));

        //String tableau_ticket = "";
        String my_line;
        while ((my_line = in.readLine()) != null) {
            tableau_ticket = my_line;        	
        }
        in.close();
        
        // return the ticket
        return tableau_ticket;
	}

	
	
	////////////////////////
	// USERS
	////////////////////////
	
	// tsGetUserIdByName
	public String tsGetUserIdByName(String user_name) throws Exception {
		String user_id = "";
		String[] users = new String[20];

		// get the list of users
		users = tsGetUsersFromSite();
		int user_count = users.length;
		int mycnt = 0;
		
		while(mycnt < user_count){
			if(users[mycnt]!=null){
				//System.out.println(users[mycnt]);
				String m_user_id = users[mycnt].split(",")[0];
				String m_user_name = users[mycnt].split(",")[1];
								
				if(user_name.equals(m_user_name)){
					user_id = m_user_id;
				}
			}
			mycnt++;
		}
		
		return user_id;
	}
	
	// tsGetUserNameById
	public String tsGetUserNameById(String user_id) throws Exception {
		String user_name = "";
		String[] users = new String[20];

		// get the list of users
		users = tsGetUsersFromSite();
		int user_count = users.length;
		int mycnt = 0;
		
		while(mycnt < user_count){
			if(users[mycnt]!=null){
				//System.out.println(users[mycnt]);
				String m_user_id = users[mycnt].split(",")[0];
				String m_user_name = users[mycnt].split(",")[1];
								
				if(user_id.equals(m_user_id)){
					user_name = m_user_name;
				}
			}
			mycnt++;
		}
		
		return user_name;
	}

	// tsGetUsersFromSite
	public String[] tsGetUsersFromSite() throws Exception {
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String[] full_token = new String[3]; 
		String xml_line = "";
		String[] user_full = new String[20];
		
		// called the function here
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// now we post the request for the users on a site
        // now send the new header request
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/users");
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));
        String my_line;
        while ((my_line = in.readLine()) != null) {
        	xml_line = xml_line + my_line;        	
        }
        in.close();    		
		
        // now lets parse the xml and get the response
        DocumentBuilderFactory xparse = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        
        try {           	
        	db = xparse.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml_line));
            try {
                Document xdoc = db.parse(is);
                xdoc.getDocumentElement().normalize();
                
                NodeList nList = xdoc.getElementsByTagName("user");
                int user_count = nList.getLength();
                int mycnt=0;
                
                while(mycnt < (user_count))
                {
                	String user_id = "";
                	String user_name = "";
                	String user_role = "";
                	
                    Node nNode = nList.item(mycnt);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			user_id = eElement.getAttribute("id");
            			user_name = eElement.getAttribute("name");
            			user_role = eElement.getAttribute("siteRole");

                        //System.out.println(mycnt + " - " + user_name);

            			user_full[mycnt] = user_id + "," + user_name  + "," + user_role;
                    }                    
                	mycnt++;
                }
                
            } catch (SAXException e) {
                // handle SAXException
            } catch (IOException e) {
                // handle IOException
            }
        } catch (ParserConfigurationException e1) {
            // handle ParserConfigurationException
        	System.out.println(e1);
        }        
        
		return user_full;		
	}	

	// tsGetUserById
	public String[] tsGetUserById(String user_id) throws Exception {
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String[] full_token = new String[3]; 
		String xml_line = "";
		String[] user_full = new String[20];
		
		// called the function here
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// now we post the request for the users on a site
        // now send the new header request
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/users/"+user_id);
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));
        String my_line;
        while ((my_line = in.readLine()) != null) {
        	xml_line = xml_line + my_line;        	
        }
        in.close();    		
		
        // now lets parse the xml and get the response
        DocumentBuilderFactory xparse = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        
        try {           	
        	db = xparse.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml_line));
            try {
                Document xdoc = db.parse(is);
                xdoc.getDocumentElement().normalize();
                
                NodeList nList = xdoc.getElementsByTagName("user");
                int user_count = nList.getLength();
                int mycnt=0;
                
                while(mycnt < (user_count))
                {
                	String muser_id = "";
                	String user_name = "";
                	String user_role = "";
                	String lastLogin = "";
                	String fullName = "";
                	
                    Node nNode = nList.item(mycnt);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			muser_id = eElement.getAttribute("id");
            			user_name = eElement.getAttribute("name");
            			user_role = eElement.getAttribute("siteRole");
            			lastLogin = eElement.getAttribute("lastLogin");
            			fullName = eElement.getAttribute("fullName");


            			user_full[mycnt] = user_id + "," + user_name  + "," + user_role  + "," + lastLogin  + "," + fullName;
                    }                    
                	mycnt++;
                }
                
            } catch (SAXException e) {
                // handle SAXException
            } catch (IOException e) {
                // handle IOException
            }
        } catch (ParserConfigurationException e1) {
            // handle ParserConfigurationException
        	System.out.println(e1);
        }        
        
		return user_full;		
	}	
	
	// tsGetGroupsFromSite
	public String[] tsGetGroupsFromSite() throws Exception {
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String[] full_token = new String[3]; 
		String xml_line = "";
		String[] user_full = new String[20];
		
		// called the function here
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// now we post the request for the users on a site
        // now send the new header request
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/groups/");
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));
        String my_line;
        while ((my_line = in.readLine()) != null) {
        	xml_line = xml_line + my_line;        	
        }
        in.close();    		
		
        // now lets parse the xml and get the response
        DocumentBuilderFactory xparse = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        
        try {           	
        	db = xparse.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml_line));
            try {
                Document xdoc = db.parse(is);
                xdoc.getDocumentElement().normalize();
                
                NodeList nList = xdoc.getElementsByTagName("group");
                int user_count = nList.getLength();
                int mycnt=0;
                
                while(mycnt < (user_count))
                {
                	String user_id = "";
                	String user_name = "";
                	
                    Node nNode = nList.item(mycnt);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			user_id = eElement.getAttribute("id");
            			user_name = eElement.getAttribute("name");

                        //System.out.println(mycnt + " - " + user_name);

            			user_full[mycnt] = user_id + "," + user_name ;
                    }                    
                	mycnt++;
                }
                
            } catch (SAXException e) {
                // handle SAXException
            } catch (IOException e) {
                // handle IOException
            }
        } catch (ParserConfigurationException e1) {
            // handle ParserConfigurationException
        	System.out.println(e1);
        }        
        
		return user_full;		
	}		

	// tsGetGroupIdByName
	public String tsGetGroupIdByName(String group_name) throws Exception {
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String[] full_token = new String[3]; 
		String xml_line = "";
		String[] user_full = new String[20];
		
		// called the function here
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// now we post the request for the users on a site
        // now send the new header request
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/groups/");
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));
        String my_line;
        while ((my_line = in.readLine()) != null) {
        	xml_line = xml_line + my_line;        	
        }
        in.close();    		
		
        // now lets parse the xml and get the response
        DocumentBuilderFactory xparse = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        
        try {           	
        	db = xparse.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml_line));
            try {
                Document xdoc = db.parse(is);
                xdoc.getDocumentElement().normalize();
                
                NodeList nList = xdoc.getElementsByTagName("group");
                int user_count = nList.getLength();
                int mycnt=0;
                
                while(mycnt < (user_count))
                {
                	String user_id = "";
                	String user_name = "";
                	
                    Node nNode = nList.item(mycnt);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			user_id = eElement.getAttribute("id");
            			user_name = eElement.getAttribute("name");

                        if(user_name.equals(group_name))
                        {
                        	tableau_ticket = user_id;
                        }
                    }                    
                	mycnt++;
                }
                
            } catch (SAXException e) {
                // handle SAXException
            } catch (IOException e) {
                // handle IOException
            }
        } catch (ParserConfigurationException e1) {
            // handle ParserConfigurationException
        	System.out.println(e1);
        }        
        
		return tableau_ticket;		
	}			

	// tsGetGroupIdByName
	public String tsGetGroupNameById(String group_id) throws Exception {
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String[] full_token = new String[3]; 
		String xml_line = "";
		String[] user_full = new String[20];
		
		// called the function here
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// now we post the request for the users on a site
        // now send the new header request
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/groups/");
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));
        String my_line;
        while ((my_line = in.readLine()) != null) {
        	xml_line = xml_line + my_line;        	
        }
        in.close();    		
		
        // now lets parse the xml and get the response
        DocumentBuilderFactory xparse = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        
        try {           	
        	db = xparse.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml_line));
            try {
                Document xdoc = db.parse(is);
                xdoc.getDocumentElement().normalize();
                
                NodeList nList = xdoc.getElementsByTagName("group");
                int user_count = nList.getLength();
                int mycnt=0;
                
                while(mycnt < (user_count))
                {
                	String user_id = "";
                	String user_name = "";
                	
                    Node nNode = nList.item(mycnt);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			user_id = eElement.getAttribute("id");
            			user_name = eElement.getAttribute("name");

                        if(user_id.equals(group_id))
                        {
                        	tableau_ticket = user_name;
                        }
                    }                    
                	mycnt++;
                }
                
            } catch (SAXException e) {
                // handle SAXException
            } catch (IOException e) {
                // handle IOException
            }
        } catch (ParserConfigurationException e1) {
            // handle ParserConfigurationException
        	System.out.println(e1);
        }        
        
		return tableau_ticket;		
	}			
	
	// tsAddGroup
	public String tsAddGroup(String group_name) throws Exception {
		String[] full_token = new String[3];
		String tableau_ticket = "";
		String tableau_site = "";
		String xmlString = "";
		String xml_line = "";
		String response = "";
		
        //Creating an empty XML Document
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        //Creating the XML tree

        //create the root element and add it to the document
        Element root = doc.createElement("tsRequest");
        doc.appendChild(root);

        //create child element, add an attribute, and add to root
        Element child = doc.createElement("group");
        child.setAttribute("name", group_name);
        root.appendChild(child);

        //Output the XML to a string
        //set up a transformer
        TransformerFactory transfac = TransformerFactory.newInstance();
        Transformer trans = transfac.newTransformer();
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        trans.setOutputProperty(OutputKeys.INDENT, "yes");

        //create string from xml tree
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        DOMSource source = new DOMSource(doc);
        trans.transform(source, result);
        xmlString = sw.toString();		
		
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		
		// call this function from your app code
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/groups/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");  
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        // prepare the postvars
        OutputStreamWriter out = new OutputStreamWriter(
                                         connection.getOutputStream());
        out.write(xmlString);
        out.close();      
        
        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));

        //String tableau_ticket = "";
        String my_line;
        while ((my_line = in.readLine()) != null) {
        	xml_line = xml_line + my_line;        	
        }
        in.close();
        
        // now lets parse the xml and get the response
        DocumentBuilderFactory xparse = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        
        try {           	
        	db = xparse.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml_line));
            try {
                Document xdoc = db.parse(is);
                xdoc.getDocumentElement().normalize();
                
                NodeList nList = xdoc.getElementsByTagName("group");
                int user_count = nList.getLength();
                int mycnt=0;
                
                while(mycnt < (user_count))
                {
                    Node nNode = nList.item(mycnt);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			response = eElement.getAttribute("id");
                    }                    
                	mycnt++;
                }
                
            } catch (SAXException e) {
                // handle SAXException
            } catch (IOException e) {
                // handle IOException
            }
        } catch (ParserConfigurationException e1) {
            // handle ParserConfigurationException
        	System.out.println(e1);
        } 
                
        
        // return the ticket
        return response;
	}

	// tsAddUser
	public String tsAddUser(String user_name, String fullName, String email, String password, String site_role) throws Exception {
		String results = "";
		
		// add the user
		results = tsAddUserToSite( user_name, site_role);
		
		// update the user
		String new_results = tsUpdateUser(results, fullName, email, password, site_role);
		
		return results;
	}
	
	// tsAddUserToSite
	public String tsAddUserToSite(String user_name, String site_role) throws Exception {
		String[] full_token = new String[3];
		String tableau_ticket = "";
		String tableau_site = "";
		String xmlString = "";
		String response = "";
		
        //Creating an empty XML Document
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        //Creating the XML tree

        //create the root element and add it to the document
        Element root = doc.createElement("tsRequest");
        doc.appendChild(root);

        //create child element, add an attribute, and add to root
        Element child = doc.createElement("user");
        child.setAttribute("name", user_name);
        child.setAttribute("siteRole", site_role);
        root.appendChild(child);

        //Output the XML to a string
        //set up a transformer
        TransformerFactory transfac = TransformerFactory.newInstance();
        Transformer trans = transfac.newTransformer();
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        trans.setOutputProperty(OutputKeys.INDENT, "yes");

        //create string from xml tree
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        DOMSource source = new DOMSource(doc);
        trans.transform(source, result);
        xmlString = sw.toString();		
		
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		
		// call this function from your app code
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/users/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");  
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        // prepare the postvars
        OutputStreamWriter out = new OutputStreamWriter(
                                         connection.getOutputStream());
        out.write(xmlString);
        out.close();      
        
        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));

        //String tableau_ticket = "";
        String my_line;
        xmlString = "";
        while ((my_line = in.readLine()) != null) {
            xmlString = xmlString + my_line;        	
        }
        in.close();
        
        // get the user_id for the new record
        
        // now lets parse the xml and get the response
        DocumentBuilderFactory xparse = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        
        try {           	
        	db = xparse.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xmlString));
            try {
                Document xdoc = db.parse(is);
                xdoc.getDocumentElement().normalize();
                
                NodeList nList = xdoc.getElementsByTagName("user");
                int user_count = nList.getLength();
                int mycnt=0;
                
                while(mycnt < (user_count))
                {
                    Node nNode = nList.item(mycnt);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			response = eElement.getAttribute("id");
                    }                    
                	mycnt++;
                }
                
            } catch (SAXException e) {
                // handle SAXException
            } catch (IOException e) {
                // handle IOException
            }
        } catch (ParserConfigurationException e1) {
            // handle ParserConfigurationException
        	System.out.println(e1);
        } 
        
        // return the ticket
        return response;
	}

	// tsAddUserToGroup
	public String tsAddUserToGroup( String group_id, String user_id) throws Exception {
		String[] full_token = new String[3];
		String tableau_ticket = "";
		String tableau_site = "";
		String xmlString = "";
		
        //Creating an empty XML Document
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        //Creating the XML tree

        //create the root element and add it to the document
        Element root = doc.createElement("tsRequest");
        doc.appendChild(root);

        //create child element, add an attribute, and add to root
        Element child = doc.createElement("user");
        child.setAttribute("id", user_id);
        root.appendChild(child);

        //Output the XML to a string
        //set up a transformer
        TransformerFactory transfac = TransformerFactory.newInstance();
        Transformer trans = transfac.newTransformer();
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        trans.setOutputProperty(OutputKeys.INDENT, "yes");

        //create string from xml tree
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        DOMSource source = new DOMSource(doc);
        trans.transform(source, result);
        xmlString = sw.toString();		
		
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		
		// call this function from your app code
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/groups/"+group_id+"/users/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");  
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        // prepare the postvars
        OutputStreamWriter out = new OutputStreamWriter(
                                         connection.getOutputStream());
        out.write(xmlString);
        out.close();      
        
        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));

        //String tableau_ticket = "";
        String my_line;
        while ((my_line = in.readLine()) != null) {
            tableau_ticket = tableau_ticket + my_line;        	
        }
        in.close();
        
        
        // return the ticket
        return "success";
	}

	// tsDeleteUserFromSite
	public void tsDeleteGroup(String group_id) throws Exception {
		String[] full_token = new String[3];
		String tableau_ticket = "";
		String tableau_site = "";
		String xmlString = "";
				
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		
		// call this function from your app code
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/groups/"+group_id);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("DELETE");        
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
//        connection.setDoOutput(true);
        
        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));

	}
	
	// tsDeleteUserFromSite
	public void tsDeleteUserFromSite(String user_id) throws Exception {
		String[] full_token = new String[3];
		String tableau_ticket = "";
		String tableau_site = "";
		String xmlString = "";
				
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		
		// call this function from your app code
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/users/"+user_id);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("DELETE");        
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
//        connection.setDoOutput(true);
        
        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));

	}

	// tsDeleteUserFromGroup
	public void tsDeleteUserFromGroup( String user_id, String group_id) throws Exception {
		String[] full_token = new String[3];
		String tableau_ticket = "";
		String tableau_site = "";
		String xmlString = "";
				
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		
		// call this function from your app code
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/groups/"+group_id+"/users/"+user_id);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("DELETE");        
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
//        connection.setDoOutput(true);
        
        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));

	}
	
	// tsUpdateUser
	public  String tsUpdateUser(String user_id, String fullName, String email, String password, String site_role) throws Exception {
		String[] full_token = new String[3];
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String tableau_user_name = "";
		String tableau_user_id = "";
		String xmlString = "";
		String xml_line = "";
		
        //Creating an empty XML Document
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        //Creating the XML tree

        //create the root element and add it to the document
        Element root = doc.createElement("tsRequest");
        doc.appendChild(root);

        //create child element, add an attribute, and add to root
        Element child = doc.createElement("user");
        child.setAttribute("fullName", fullName);
        child.setAttribute("email", email);
        child.setAttribute("password", password);
        child.setAttribute("siteRole", site_role);
        root.appendChild(child);

        //Output the XML to a string
        //set up a transformer
        TransformerFactory transfac = TransformerFactory.newInstance();
        Transformer trans = transfac.newTransformer();
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        trans.setOutputProperty(OutputKeys.INDENT, "yes");

        //create string from xml tree
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        DOMSource source = new DOMSource(doc);
        trans.transform(source, result);
        xmlString = sw.toString();		
		
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// call this function from your app code
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/users/"+user_id);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("PUT");        
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);


        // prepare the postvars
        OutputStreamWriter out = new OutputStreamWriter(
                                         connection.getOutputStream());
        out.write(xmlString);
        out.close();      
        
        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));

        String my_line;
        while ((my_line = in.readLine()) != null) {
        	xml_line = xml_line + my_line;        	
        }
        in.close();
        
        // return the ticket
        return xml_line;
	}

	// tsGetUsersFromGroup
	public  String[] tsGetUsersFromGroup(String group_id) throws Exception {
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String[] full_token = new String[3]; 
		String xml_line = "";
		String[] user_full = new String[20];
		
		// called the function here
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// now we post the request for the users on a site
        // now send the new header request
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/groups/"+group_id+"/users/");
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));
        String my_line;
        while ((my_line = in.readLine()) != null) {
        	xml_line = xml_line + my_line;        	
        }
        in.close();    		
		
        // now lets parse the xml and get the response
        DocumentBuilderFactory xparse = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        
        try {           	
        	db = xparse.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml_line));
            try {
                Document xdoc = db.parse(is);
                xdoc.getDocumentElement().normalize();
                
                NodeList nList = xdoc.getElementsByTagName("user");
                int user_count = nList.getLength();
                int mycnt=0;
                
                while(mycnt < (user_count))
                {
                	String user_id = "";
                	String user_name = "";
                	String user_role = "";
                	
                    Node nNode = nList.item(mycnt);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			user_id = eElement.getAttribute("id");
            			user_name = eElement.getAttribute("name");
            			user_role = eElement.getAttribute("siteRole");

                        //System.out.println(mycnt + " - " + user_name);

            			user_full[mycnt] = user_id + "," + user_name  + "," + user_role;
                    }                    
                	mycnt++;
                }
                
            } catch (SAXException e) {
                // handle SAXException
            } catch (IOException e) {
                // handle IOException
            }
        } catch (ParserConfigurationException e1) {
            // handle ParserConfigurationException
        	System.out.println(e1);
        }        
        
		return user_full;		
	}	
	
	// tsUpdateGroup
	public String tsUpdateGroup(String group_id, String group_name) throws Exception {
		String[] full_token = new String[3];
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String tableau_user_name = "";
		String tableau_user_id = "";
		String xmlString = "";
		
        //Creating an empty XML Document
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        //Creating the XML tree

        //create the root element and add it to the document
        Element root = doc.createElement("tsRequest");
        doc.appendChild(root);

        //create child element, add an attribute, and add to root
        Element child = doc.createElement("group");
        child.setAttribute("name", group_name);
        root.appendChild(child);
        
        //Output the XML to a string
        //set up a transformer
        TransformerFactory transfac = TransformerFactory.newInstance();
        Transformer trans = transfac.newTransformer();
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        trans.setOutputProperty(OutputKeys.INDENT, "yes");

        //create string from xml tree
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        DOMSource source = new DOMSource(doc);
        trans.transform(source, result);
        xmlString = sw.toString();		
		
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// call this function from your app code
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/groups/"+group_id);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("PUT"); 
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);


        // prepare the postvars
        OutputStreamWriter out = new OutputStreamWriter(
                                         connection.getOutputStream());
        out.write(xmlString);
        out.close();      
        
        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));

        //String tableau_ticket = "";
        String my_line;
        while ((my_line = in.readLine()) != null) {
            tableau_ticket = my_line;        	
        }
        in.close();
        
        // return the ticket
        return tableau_ticket;
	}
	
	
	////////////////////////
	// DATASOURCES
	////////////////////////
	
	// tsGetDataSources
	public String[] tsGetDataSources() throws Exception {
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String[] full_token = new String[3]; 
		String xml_line = "";
		String[] user_full = new String[20];
		
		// called the function here
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// now we post the request for the users on a site
        // now send the new header request
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/datasources");
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));
        String my_line;
        while ((my_line = in.readLine()) != null) {
        	xml_line = xml_line + my_line;        	
        }
        in.close();    		
		
        // now lets parse the xml and get the response
        DocumentBuilderFactory xparse = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        
        try {           	
        	db = xparse.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml_line));
            try {
                Document xdoc = db.parse(is);
                xdoc.getDocumentElement().normalize();
                
                NodeList nList = xdoc.getElementsByTagName("datasource");
                int user_count = nList.getLength();
                int mycnt=0;
                
                while(mycnt < (user_count))
                {
                	String user_id = "";
                	String user_name = "";
                	String user_role = "";
                	
                    Node nNode = nList.item(mycnt);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			user_id = eElement.getAttribute("id");
            			user_name = eElement.getAttribute("name");
            			user_role = eElement.getAttribute("type");

                        //System.out.println(mycnt + " - " + user_name);

            			user_full[mycnt] = user_id + "," + user_name  + "," + user_role;
                    }                    
                	mycnt++;
                }
                
            } catch (SAXException e) {
                // handle SAXException
            } catch (IOException e) {
                // handle IOException
            }
        } catch (ParserConfigurationException e1) {
            // handle ParserConfigurationException
        	System.out.println(e1);
        }        
        
		return user_full;		
	}	

	// tsGetDataSourceNameById
	public String tsGetDataSourceNameById(String conn_id) throws Exception {
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String new_name = "";
		String tableau_user_name = "";
		String tableau_user_id = "";
		String[] full_token = new String[3]; 
		String xml_line = "";
		String[] user_full = new String[20];
		
		// called the function here
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// get the user id
//		tableau_user_id = getUserId(user_name, ts_url, ts_user, ts_pass);
		
		// now we post the request for the users on a site
        // now send the new header request
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/datasources");
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));
        String my_line;
        while ((my_line = in.readLine()) != null) {
        	xml_line = xml_line + my_line;        	
        }
        in.close();    		
		
        //System.out.println(xml_line);
        
        // now lets parse the xml and get the response
        DocumentBuilderFactory xparse = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        
        try {           	
        	db = xparse.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml_line));
            try {
                Document xdoc = db.parse(is);
                xdoc.getDocumentElement().normalize();
                
                NodeList nList = xdoc.getElementsByTagName("datasource");
                int user_count = nList.getLength();
                int mycnt=0;
                
                while(mycnt < (user_count))
                {
                	String connect_id = "";
                	String c_type = "";
                	String c_name = "";
                	
                    Node nNode = nList.item(mycnt);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			connect_id = eElement.getAttribute("id");
            			c_type = eElement.getAttribute("type");
            			c_name = eElement.getAttribute("name");

                        //System.out.println(mycnt + " - " + user_name);

            			//tableau_user_id = connect_id + "," + c_name + "," + c_type;
            			if(conn_id.equals(connect_id)){
            				new_name = c_name;
            			}
            			
                    }                    
                	mycnt++;
                }
                
            } catch (SAXException e) {
                // handle SAXException
            } catch (IOException e) {
                // handle IOException
            }
        } catch (ParserConfigurationException e1) {
            // handle ParserConfigurationException
        	System.out.println(e1);
        }        
        
		return new_name;					
	}

	// tsGetDataSourceIdByName
	public String tsGetDataSourceIdByName(String conn_name) throws Exception {
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String new_name = "";
		String tableau_user_name = "";
		String tableau_user_id = "";
		String[] full_token = new String[3]; 
		String xml_line = "";
		String[] user_full = new String[20];
		
		// called the function here
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// get the user id
//		tableau_user_id = getUserId(user_name, ts_url, ts_user, ts_pass);
		
		// now we post the request for the users on a site
        // now send the new header request
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/datasources");
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));
        String my_line;
        while ((my_line = in.readLine()) != null) {
        	xml_line = xml_line + my_line;        	
        }
        in.close();    		
		
        //System.out.println(xml_line);
        
        // now lets parse the xml and get the response
        DocumentBuilderFactory xparse = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        
        try {           	
        	db = xparse.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml_line));
            try {
                Document xdoc = db.parse(is);
                xdoc.getDocumentElement().normalize();
                
                NodeList nList = xdoc.getElementsByTagName("datasource");
                int user_count = nList.getLength();
                int mycnt=0;
                
                while(mycnt < (user_count))
                {
                	String connect_id = "";
                	String c_type = "";
                	String c_name = "";
                	
                    Node nNode = nList.item(mycnt);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			connect_id = eElement.getAttribute("id");
            			c_type = eElement.getAttribute("type");
            			c_name = eElement.getAttribute("name");

                        //System.out.println(mycnt + " - " + user_name);

            			//tableau_user_id = connect_id + "," + c_name + "," + c_type;
            			if(conn_name.equals(c_name)){
            				new_name = connect_id;
            			}
            			
                    }                    
                	mycnt++;
                }
                
            } catch (SAXException e) {
                // handle SAXException
            } catch (IOException e) {
                // handle IOException
            }
        } catch (ParserConfigurationException e1) {
            // handle ParserConfigurationException
        	System.out.println(e1);
        }        
        
		return new_name;					
	}
	
	// tsAddDataSourcePermission
	public String tsAddDataSourcePermission(String datasource_id, String user_id, String group_id, String capability_name, String capability_mode) throws Exception {
		String[] full_token = new String[3];
		String tableau_ticket = "";
		String tableau_site = "";
		String xmlString = "";
		
        //Creating an empty XML Document
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        //Creating the XML tree

        //create the root element and add it to the document
        Element root = doc.createElement("tsRequest");
        doc.appendChild(root);

        //create child element, add an attribute, and add to root
        Element child = doc.createElement("permissions");
        root.appendChild(child);

        Element child2 = doc.createElement("datasource");
        child2.setAttribute("id", datasource_id);
        child.appendChild(child2);

        Element child3 = doc.createElement("granteeCapabilities");
        child.appendChild(child3);

        // if this is a group
        if(group_id.length() > 0)
        {
            Element child4 = doc.createElement("group");
            child4.setAttribute("id", group_id);
            child3.appendChild(child4);        	
        }
        else
        {
        	// else this is a user
            Element child4 = doc.createElement("user");
            child4.setAttribute("id", user_id);
            child3.appendChild(child4);        	        	
        }
        
        // set the capability
        Element child5 = doc.createElement("capabilities");
        child3.appendChild(child5);
        
        Element child6 = doc.createElement("capability");
        child6.setAttribute("name", capability_name);
        child6.setAttribute("mode", capability_mode);
        child5.appendChild(child6);        	        	
        
        //Output the XML to a string
        //set up a transformer
        TransformerFactory transfac = TransformerFactory.newInstance();
        Transformer trans = transfac.newTransformer();
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        trans.setOutputProperty(OutputKeys.INDENT, "yes");

        //create string from xml tree
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        DOMSource source = new DOMSource(doc);
        trans.transform(source, result);
        xmlString = sw.toString();		
		
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		
		// call this function from your app code
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/datasources/"+datasource_id+"/permissions/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("PUT");  
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        // prepare the postvars
        OutputStreamWriter out = new OutputStreamWriter(
                                         connection.getOutputStream());
        out.write(xmlString);
        out.close();      
        
        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));

        //String tableau_ticket = "";
        String my_line;
        while ((my_line = in.readLine()) != null) {
            tableau_ticket = my_line;        	
        }
        in.close();
                
        // return the ticket
        return tableau_ticket;
	}

	// tsDeleteDataSource
	public void tsDeleteDataSource(String datasource_id) throws Exception {
		String[] full_token = new String[3];
		String tableau_ticket = "";
		String tableau_site = "";
		String xmlString = "";
				
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		
		// call this function from your app code
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/datasources/"+datasource_id);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("DELETE");        
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
//        connection.setDoOutput(true);
        
        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));

	}

	// tsDeleteDataSourcePermission
	public void tsDeleteDataSourcePermission(String datasource_id, String user_id, String group_id, String capability_name, String capability_mode) throws Exception {
		String[] full_token = new String[3];
		String tableau_ticket = "";
		String tableau_site = "";
		String xmlString = "";
				
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		URL url;
		
		// call this function from your app code
		if(user_id.length() > 0)
		{
			url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/datasources/"+datasource_id + "/permissions/" + "users/" + user_id +"/"+ capability_name + "/" + capability_mode);
		}
		else
		{
			url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/datasources/"+datasource_id + "/permissions/" + "groups/" + group_id +"/"+ capability_name + "/" + capability_mode);			
		}
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("DELETE");        
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
//        connection.setDoOutput(true);
        
        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));

	}
	
	// tsUpdateDataSource
	public String tsUpdateDataSource(String datasource_id, String datasource_name, String project_id, String owner_id) throws Exception {
		String[] full_token = new String[3];
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String tableau_user_name = "";
		String tableau_user_id = "";
		String xmlString = "";
		
        //Creating an empty XML Document
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        //Creating the XML tree

        //create the root element and add it to the document
        Element root = doc.createElement("tsRequest");
        doc.appendChild(root);

        Element child = doc.createElement("datasource");
        child.setAttribute("name", datasource_name);
        root.appendChild(child);
        
        //create child element, add an attribute, and add to root
        Element child2 = doc.createElement("project");
        child2.setAttribute("id", project_id);
        child.appendChild(child2);

        Element child3 = doc.createElement("owner");
        child3.setAttribute("id", owner_id);
        child.appendChild(child3);
        
        //Output the XML to a string
        //set up a transformer
        TransformerFactory transfac = TransformerFactory.newInstance();
        Transformer trans = transfac.newTransformer();
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        trans.setOutputProperty(OutputKeys.INDENT, "yes");

        //create string from xml tree
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        DOMSource source = new DOMSource(doc);
        trans.transform(source, result);
        xmlString = sw.toString();		
		
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// call this function from your app code
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/datasources/"+datasource_id);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("PUT"); 
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);


        // prepare the postvars
        OutputStreamWriter out = new OutputStreamWriter(
                                         connection.getOutputStream());
        out.write(xmlString);
        out.close();      
        
        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));

        //String tableau_ticket = "";
        String my_line;
        while ((my_line = in.readLine()) != null) {
            tableau_ticket = my_line;        	
        }
        in.close();
        
        // return the ticket
        return tableau_ticket;
	}
	
	// tsGetPermissionsFromDataSource
	public String[] tsGetPermissionsFromDataSource(String datasource_id) throws Exception {
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String tableau_user_name = "";
		String tableau_user_id = "";
		String[] full_token = new String[3]; 
		String xml_line = "";
		String[] user_full = new String[200];
		
		// called the function here
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// get the user id
//		tableau_user_id = getUserId(user_name, ts_url, ts_user, ts_pass);
		
		// now we post the request for the users on a site
        // now send the new header request
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/datasources/"+datasource_id+"/permissions");
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));
        String my_line;
        while ((my_line = in.readLine()) != null) {
        	xml_line = xml_line + my_line;        	
        }
        in.close();    		
		
        //System.out.println(xml_line);
        
        // now lets parse the xml and get the response
        DocumentBuilderFactory xparse = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        
        try {           	
        	db = xparse.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml_line));
            try {
                Document xdoc = db.parse(is);
                xdoc.getDocumentElement().normalize();
            	String user_id = "";
            	String group_id = "";
            	String wb_id = "";
            	String wb_name = "";
            	String cap_name = "";
            	String cap_mode = "";
                
                NodeList nList = xdoc.getElementsByTagName("datasource");
                int user_count = nList.getLength();
                int mycnt=0;
                int mycnt5=0;
                
                while(mycnt < (user_count))
                {                	
                    Node nNode = nList.item(mycnt);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			String c_name = eElement.getTagName();
            			
            			if(c_name == "datasource")
            			{
            				wb_id = eElement.getAttribute("id");
            				wb_name = eElement.getAttribute("name");
            			}

                    }                    
                	mycnt++;
                }
                
                NodeList nList2 = xdoc.getElementsByTagName("user");
                int user_count2 = nList2.getLength();
                int mycnt2=0;
                
                while(mycnt2 < (user_count2))
                {
                    Node nNode = nList2.item(mycnt2);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			String c_name = eElement.getTagName();
            			
            			if(c_name == "user")
            			{
            				user_id = eElement.getAttribute("id");
            			}
                    }                    
                	mycnt2++;
                }
                //
                NodeList nList3 = xdoc.getElementsByTagName("group");
                int user_count3 = nList3.getLength();
                int mycnt3=0;
                
                while(mycnt3 < (user_count3))
                {
                    Node nNode = nList3.item(mycnt3);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			String c_name = eElement.getTagName();
            			
            			if(c_name == "group")
            			{
            				group_id = eElement.getAttribute("id");
            			}
                    }                    
                	mycnt3++;
                }

                // 
                NodeList nList4 = xdoc.getElementsByTagName("capability");
                int user_count4 = nList4.getLength();
                int mycnt4=0;
                
                while(mycnt4 < (user_count4))
                {
                    Node nNode = nList4.item(mycnt4);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			String c_name = eElement.getTagName();
            			
            			if(c_name == "capability")
            			{
            				cap_name = eElement.getAttribute("name");
            				cap_mode = eElement.getAttribute("mode");
                			user_full[mycnt5] = wb_id + "," + wb_name + "," + user_id + "," + group_id + "," + cap_name + "," + cap_mode;
                        	mycnt5++;
            			}
                    }                    
                	mycnt4++;
                }

                } catch (SAXException e) {
                // handle SAXException
            } catch (IOException e) {
                // handle IOException
            }
        } catch (ParserConfigurationException e1) {
            // handle ParserConfigurationException
        	System.out.println(e1);
        }        
        
		return user_full;					
	}
	
	// tsDownloadDataSource 
	public String tsDownloadDataSource(String datasource_name, String save_path) throws Exception {
		String[] full_token = new String[3];
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String tableau_user_name = "";
		String tableau_user_id = "";
		
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// call this function from your app code
		String datasource_id = tsGetDataSourceIdByName(datasource_name);
		
		 String fileName = save_path; //The file that will be saved on your computer
		 URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/datasources/"+datasource_id+"/content"); //The file that you want to download
			URLConnection connection = url.openConnection();
			connection.setRequestProperty("Content-Type", "text/xml");
			connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
			connection.setDoOutput(true);
			 
			 //Code to download
			 InputStream in = new BufferedInputStream(connection.getInputStream());
		 ByteArrayOutputStream out = new ByteArrayOutputStream();
		 byte[] buf = new byte[1024];
		 int n = 0;
		 while (-1!=(n=in.read(buf)))
		 {
		    out.write(buf, 0, n);
		 }
		 out.close();
		 in.close();
		 byte[] response = out.toByteArray();

		 FileOutputStream fos = new FileOutputStream(fileName);
		 fos.write(response);
		 fos.close();
		 //End download code        
        
        
        // return the ticket
        return tableau_ticket;
	}

	// tsPublishDataSource 
	public String tsPublishDataSource( String datasource_location, String datasource_name, String project_name) throws Exception {
		String[] full_token = new String[3];
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String tableau_user_name = "";
		String tableau_user_id = "";
		String writer_res = "";
		String xml_line = "";
		
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// call this function from your app code
		//String workbook_id = tsGetWorkbookIdByName(workbook_name);

		String project_id = tsGetProjectIdByName(project_name);
		
		// find out what type of file this is
		String workbook_type = "";
		int responseCode = 0;
		int i = datasource_location.lastIndexOf('.');
		if (i > 0) {
			workbook_type = datasource_location.substring(i+1);
		}
		
		// get the file size
		File wbFile = new File(datasource_location);
		long wbSize = wbFile.length();
        String my_id = "";

		if(wbSize > 60000000)
		{
			// we need to chunk the file
			
		}
		else
		{
			// we can just post it directly			
			String charset = "UTF-8";
			String param = "value";
			
			String boundary = Long.toHexString(System.currentTimeMillis()); // Just generate some unique random value.
			String CRLF = "\r\n"; // Line separator required by multipart/form-data.

			URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/datasources?overwrite=true");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	        connection.setRequestMethod("POST");  
	        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);	        
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", "multipart/mixed; boundary=" + boundary);

			try (
			    OutputStream output = connection.getOutputStream();
			    PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, charset), true);
			) {
			    // Send normal param.
			    writer.append("--" + boundary).append(CRLF);
			    writer.append("Content-Disposition: name=\"request_payload\"").append(CRLF);
			    writer.append("Content-Type: text/xml;").append(CRLF);
			    writer.append(CRLF).append("<tsRequest><datasource name = \"" + datasource_name + "\" showTabs = \"true\"><project id = \"" + project_id + "\" /></datasource></tsRequest>").append(CRLF).flush();

			    // Send text file.
			    writer.append("--" + boundary).append(CRLF);
			    writer.append("Content-Disposition: name=\"tableau_datasource\"; filename=\"" + wbFile.getName() + "\"").append(CRLF);
			    writer.append("Content-Type: application/octet-stream;").append(CRLF); // Text file itself must be saved in this charset!
			    writer.append(CRLF).flush();
			    Files.copy(wbFile.toPath(), output);
			    output.flush(); // Important before continuing with writer!
			    writer.append(CRLF).flush(); // CRLF is important! It indicates end of boundary.

			    // End of multipart/form-data.
			    writer.append("--" + boundary + "--").flush();
			    
			}

			// Request is lazily fired whenever you need to obtain information about response.
			responseCode = ((HttpURLConnection) connection).getResponseCode();
	        // parse and get the ticket

	        BufferedReader in = new BufferedReader(
	        						new InputStreamReader(
	        								connection.getInputStream()));
	        String my_line;
	        while ((my_line = in.readLine()) != null) {
	        	xml_line = xml_line + my_line;        	
	        }
	        in.close();    		
			
	        //System.out.println(xml_line);
	        
	        // now lets parse the xml and get the response
	        DocumentBuilderFactory xparse = DocumentBuilderFactory.newInstance();
	        DocumentBuilder db = null;
	        
	        try {           	
	        	db = xparse.newDocumentBuilder();
	            InputSource is = new InputSource();
	            is.setCharacterStream(new StringReader(xml_line));
	            try {
	                Document xdoc = db.parse(is);
	                xdoc.getDocumentElement().normalize();
	                
	                NodeList nList = xdoc.getElementsByTagName("datasource");
	                int user_count = nList.getLength();
	                int mycnt=0;
	                
	                while(mycnt < (user_count))
	                {
	                	String wb_id = "";
	                	String wb_name = "";
	                	String workbook_contenturl = "";
	                	String workbook_showtabs = "";
	                	
	                    Node nNode = nList.item(mycnt);
	                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
	            			Element eElement = (Element) nNode;
	            			wb_id = eElement.getAttribute("id");
            				my_id = wb_id;
	                    }                    
	                	mycnt++;
	                }
	                
	            } catch (SAXException e) {
	                // handle SAXException
	            } catch (IOException e) {
	                // handle IOException
	            }
	        } catch (ParserConfigurationException e1) {
	            // handle ParserConfigurationException
	        	System.out.println(e1);
	        }        
		}		
		
        //tableau_ticket = String.valueOf(responseCode);
		
        // return the ticket
        return my_id;
	}
	
	// tsUpdateDataSourceConnection
	public String tsUpdateDataSourceConnection(String datasource_id, String serverAddress, String serverPort, String userName, String password) throws Exception {
		String[] full_token = new String[3];
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String tableau_user_name = "";
		String tableau_user_id = "";
		String xmlString = "";
		
        //Creating an empty XML Document
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        //Creating the XML tree

        //create the root element and add it to the document
        Element root = doc.createElement("tsRequest");
        doc.appendChild(root);

        //create child element, add an attribute, and add to root
        Element child = doc.createElement("connection");
        child.setAttribute("serverAddress", serverAddress);
        child.setAttribute("serverPort", serverPort	);
        child.setAttribute("userName", userName);
        child.setAttribute("password", password);
        root.appendChild(child);
        
        //Output the XML to a string
        //set up a transformer
        TransformerFactory transfac = TransformerFactory.newInstance();
        Transformer trans = transfac.newTransformer();
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        trans.setOutputProperty(OutputKeys.INDENT, "yes");

        //create string from xml tree
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        DOMSource source = new DOMSource(doc);
        trans.transform(source, result);
        xmlString = sw.toString();		
		
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// call this function from your app code
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/datasources/"+datasource_id+"/connection/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("PUT"); 
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);


        // prepare the postvars
        OutputStreamWriter out = new OutputStreamWriter(
                                         connection.getOutputStream());
        out.write(xmlString);
        out.close();      
        
        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));

        //String tableau_ticket = "";
        String my_line;
        while ((my_line = in.readLine()) != null) {
            tableau_ticket = my_line;        	
        }
        in.close();
        
        // return the ticket
        return tableau_ticket;
	}
	
		
	////////////////////////
	// PROJECTS
	////////////////////////
	
	// tsGetProjects
	public String[] tsGetProjects() throws Exception {
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String[] full_token = new String[3]; 
		String xml_line = "";
		String[] user_full = new String[20];
		
		// called the function here
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// now we post the request for the users on a site
        // now send the new header request
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/projects");
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));
        String my_line;
        while ((my_line = in.readLine()) != null) {
        	xml_line = xml_line + my_line;        	
        }
        in.close();    		
		
        // now lets parse the xml and get the response
        DocumentBuilderFactory xparse = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        
        try {           	
        	db = xparse.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml_line));
            try {
                Document xdoc = db.parse(is);
                xdoc.getDocumentElement().normalize();
                
                NodeList nList = xdoc.getElementsByTagName("project");
                int user_count = nList.getLength();
                int mycnt=0;
                
                while(mycnt < (user_count))
                {
                	String user_id = "";
                	String user_name = "";
                	String user_role = "";
                	
                    Node nNode = nList.item(mycnt);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			user_id = eElement.getAttribute("id");
            			user_name = eElement.getAttribute("name");

                        //System.out.println(mycnt + " - " + user_name);

            			user_full[mycnt] = user_id + "," + user_name  ;
                    }                    
                	mycnt++;
                }
                
            } catch (SAXException e) {
                // handle SAXException
            } catch (IOException e) {
                // handle IOException
            }
        } catch (ParserConfigurationException e1) {
            // handle ParserConfigurationException
        	System.out.println(e1);
        }        
        
		return user_full;		
	}	
		
	// tsGetProjectNameById
	public String tsGetProjectNameById(String project_id) throws Exception {
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String new_name = "";
		String tableau_user_name = "";
		String tableau_user_id = "";
		String[] full_token = new String[3]; 
		String xml_line = "";
		String[] user_full = new String[20];
		
		// called the function here
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// get the user id
//		tableau_user_id = getUserId(user_name, ts_url, ts_user, ts_pass);
		
		// now we post the request for the users on a site
        // now send the new header request
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/projects");
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));
        String my_line;
        while ((my_line = in.readLine()) != null) {
        	xml_line = xml_line + my_line;        	
        }
        in.close();    		
		
        //System.out.println(xml_line);
        
        // now lets parse the xml and get the response
        DocumentBuilderFactory xparse = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        
        try {           	
        	db = xparse.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml_line));
            try {
                Document xdoc = db.parse(is);
                xdoc.getDocumentElement().normalize();
                
                NodeList nList = xdoc.getElementsByTagName("project");
                int user_count = nList.getLength();
                int mycnt=0;
                
                while(mycnt < (user_count))
                {
                	String connect_id = "";
                	String c_type = "";
                	String c_name = "";
                	
                    Node nNode = nList.item(mycnt);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			connect_id = eElement.getAttribute("id");
            			c_name = eElement.getAttribute("name");

            			//tableau_user_id = connect_id + "," + c_name + "," + c_type;
            			if(project_id.equals(connect_id)){
            				new_name = c_name;
            			}
            			
                    }                    
                	mycnt++;
                }
                
            } catch (SAXException e) {
                // handle SAXException
            } catch (IOException e) {
                // handle IOException
            }
        } catch (ParserConfigurationException e1) {
            // handle ParserConfigurationException
        	System.out.println(e1);
        }        
        
		return new_name;					
	}

	// tsGetProjectIdByName
	public String tsGetProjectIdByName(String project_name) throws Exception {
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String new_name = "";
		String tableau_user_name = "";
		String tableau_user_id = "";
		String[] full_token = new String[3]; 
		String xml_line = "";
		String[] user_full = new String[20];
		
		// called the function here
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// get the user id
//		tableau_user_id = getUserId(user_name, ts_url, ts_user, ts_pass);
		
		// now we post the request for the users on a site
        // now send the new header request
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/projects");
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));
        String my_line;
        while ((my_line = in.readLine()) != null) {
        	xml_line = xml_line + my_line;        	
        }
        in.close();    		
		
        //System.out.println(xml_line);
        
        // now lets parse the xml and get the response
        DocumentBuilderFactory xparse = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        
        try {           	
        	db = xparse.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml_line));
            try {
                Document xdoc = db.parse(is);
                xdoc.getDocumentElement().normalize();
                
                NodeList nList = xdoc.getElementsByTagName("project");
                int user_count = nList.getLength();
                int mycnt=0;
                
                while(mycnt < (user_count))
                {
                	String connect_id = "";
                	String c_type = "";
                	String c_name = "";
                	
                    Node nNode = nList.item(mycnt);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			connect_id = eElement.getAttribute("id");
            			c_name = eElement.getAttribute("name");

                        //System.out.println(mycnt + " - " + user_name);

            			//tableau_user_id = connect_id + "," + c_name + "," + c_type;
            			if(project_name.equals(c_name)){
            				new_name = connect_id;
            			}
            			
                    }                    
                	mycnt++;
                }
                
            } catch (SAXException e) {
                // handle SAXException
            } catch (IOException e) {
                // handle IOException
            }
        } catch (ParserConfigurationException e1) {
            // handle ParserConfigurationException
        	System.out.println(e1);
        }        
        
		return new_name;					
	}
	
	// tsAddProject
	public String tsAddProject(String project_name, String project_desc) throws Exception {
		String[] full_token = new String[3];
		String tableau_ticket = "";
		String tableau_site = "";
		String xmlString = "";
		
        //Creating an empty XML Document
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        //Creating the XML tree

        //create the root element and add it to the document
        Element root = doc.createElement("tsRequest");
        doc.appendChild(root);

        //create child element, add an attribute, and add to root
        Element child = doc.createElement("project");
        child.setAttribute("name", project_name);
        child.setAttribute("description", project_desc);
        root.appendChild(child);

        //Output the XML to a string
        //set up a transformer
        TransformerFactory transfac = TransformerFactory.newInstance();
        Transformer trans = transfac.newTransformer();
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        trans.setOutputProperty(OutputKeys.INDENT, "yes");

        //create string from xml tree
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        DOMSource source = new DOMSource(doc);
        trans.transform(source, result);
        xmlString = sw.toString();		
		
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		
		// call this function from your app code
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/projects/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");  
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        // prepare the postvars
        OutputStreamWriter out = new OutputStreamWriter(
                                         connection.getOutputStream());
        out.write(xmlString);
        out.close();      
        
        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));

        //String tableau_ticket = "";
        String my_line;
        tableau_ticket = "";
        while ((my_line = in.readLine()) != null) {
            tableau_ticket = tableau_ticket + my_line;        	
        }
        in.close();
        
        // get the user_id for the new record
        
        // now lets parse the xml and get the response
        DocumentBuilderFactory xparse = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        
        try {           	
        	db = xparse.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(tableau_ticket));
            try {
                Document xdoc = db.parse(is);
                xdoc.getDocumentElement().normalize();
                
                NodeList nList = xdoc.getElementsByTagName("project");
                int user_count = nList.getLength();
                int mycnt=0;
                
                while(mycnt < (user_count))
                {
                    Node nNode = nList.item(mycnt);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			tableau_ticket = eElement.getAttribute("id");
                    }                    
                	mycnt++;
                }
                
            } catch (SAXException e) {
                // handle SAXException
            } catch (IOException e) {
                // handle IOException
            }
        } catch (ParserConfigurationException e1) {
            // handle ParserConfigurationException
        	System.out.println(e1);
        } 
        
        // return the ticket
        return tableau_ticket;
	}

	// tsAddProjectPermission
	public String tsAddProjectPermission(String project_id, String user_id, String group_id, String capability_name, String capability_mode) throws Exception {
		String[] full_token = new String[3];
		String tableau_ticket = "";
		String tableau_site = "";
		String xmlString = "";
		
        //Creating an empty XML Document
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        //Creating the XML tree

        //create the root element and add it to the document
        Element root = doc.createElement("tsRequest");
        doc.appendChild(root);

        //create child element, add an attribute, and add to root
        Element child = doc.createElement("permissions");
        root.appendChild(child);

        Element child2 = doc.createElement("project");
        child2.setAttribute("id", project_id);
        child.appendChild(child2);

        Element child3 = doc.createElement("granteeCapabilities");
        child.appendChild(child3);

        // if this is a group
        if(group_id.length() > 0)
        {
            Element child4 = doc.createElement("group");
            child4.setAttribute("id", group_id);
            child3.appendChild(child4);        	
        }
        else
        {
        	// else this is a user
            Element child4 = doc.createElement("user");
            child4.setAttribute("id", user_id);
            child3.appendChild(child4);        	        	
        }
        
        // set the capability
        Element child5 = doc.createElement("capabilities");
        child3.appendChild(child5);
        
        Element child6 = doc.createElement("capability");
        child6.setAttribute("name", capability_name);
        child6.setAttribute("mode", capability_mode);
        child5.appendChild(child6);        	        	
        
        //Output the XML to a string
        //set up a transformer
        TransformerFactory transfac = TransformerFactory.newInstance();
        Transformer trans = transfac.newTransformer();
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        trans.setOutputProperty(OutputKeys.INDENT, "yes");

        //create string from xml tree
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        DOMSource source = new DOMSource(doc);
        trans.transform(source, result);
        xmlString = sw.toString();		
		
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		
		// call this function from your app code
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/projects/"+project_id+"/permissions/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("PUT");  
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        // prepare the postvars
        OutputStreamWriter out = new OutputStreamWriter(
                                         connection.getOutputStream());
        out.write(xmlString);
        out.close();      
        
        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));

        //String tableau_ticket = "";
        String my_line;
        while ((my_line = in.readLine()) != null) {
            tableau_ticket = my_line;        	
        }
        in.close();
                
        // return the ticket
        return tableau_ticket;
	}
	
	// tsDeleteUserFromSite
	public void tsDeleteProject(String project_id) throws Exception {
		String[] full_token = new String[3];
		String tableau_ticket = "";
		String tableau_site = "";
		String xmlString = "";
				
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		
		// call this function from your app code
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/projects/"+project_id);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("DELETE");        
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
//        connection.setDoOutput(true);
        
        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));

	}

	// tsDeleteProjectPermission
	public void tsDeleteProjectPermission(String project_id, String user_id, String group_id, String capability_name, String capability_mode) throws Exception {
		String[] full_token = new String[3];
		String tableau_ticket = "";
		String tableau_site = "";
		String xmlString = "";
				
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		URL url;
		
		// call this function from your app code
		if(user_id.length() > 0)
		{
			url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/projects/"+project_id + "/permissions/" + "users/" + user_id +"/"+ capability_name + "/" + capability_mode);
		}
		else
		{
			url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/projects/"+project_id + "/permissions/" + "groups/" + group_id +"/"+ capability_name + "/" + capability_mode);			
		}
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("DELETE");        
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
//        connection.setDoOutput(true);
        
        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));

	}

	// tsUpdateProject
	public String tsUpdateProject( String project_id, String project_name, String description) throws Exception {
		String[] full_token = new String[3];
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String tableau_user_name = "";
		String tableau_user_id = "";
		String xmlString = "";
		
        //Creating an empty XML Document
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        //Creating the XML tree

        //create the root element and add it to the document
        Element root = doc.createElement("tsRequest");
        doc.appendChild(root);

        //create child element, add an attribute, and add to root
        Element child = doc.createElement("project");
        child.setAttribute("name", project_name);
        child.setAttribute("description", description);
        root.appendChild(child);

        //Output the XML to a string
        //set up a transformer
        TransformerFactory transfac = TransformerFactory.newInstance();
        Transformer trans = transfac.newTransformer();
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        trans.setOutputProperty(OutputKeys.INDENT, "yes");

        //create string from xml tree
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        DOMSource source = new DOMSource(doc);
        trans.transform(source, result);
        xmlString = sw.toString();		
		
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// call this function from your app code
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/projects/"+project_id);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("PUT"); 
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);


        // prepare the postvars
        OutputStreamWriter out = new OutputStreamWriter(
                                         connection.getOutputStream());
        out.write(xmlString);
        out.close();      
        
        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));

        //String tableau_ticket = "";
        String my_line;
        while ((my_line = in.readLine()) != null) {
            tableau_ticket = my_line;        	
        }
        in.close();
        
        // return the ticket
        return tableau_ticket;
	}
	
	// tsGetPermissionsFromProject
	public String[] tsGetPermissionsFromProject(String project_id) throws Exception {
		String tableau_ticket = "";
		String tableau_site = "";
		String tableau_user = "";
		String tableau_user_name = "";
		String tableau_user_id = "";
		String[] full_token = new String[3]; 
		String xml_line = "";
		String[] user_full = new String[200];
		
		// called the function here
		try {
			full_token = tsLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// get the token
		tableau_ticket = full_token[0];
		tableau_site = full_token[1];
		tableau_user = full_token[2];
		
		// get the user id
//		tableau_user_id = getUserId(user_name, ts_url, ts_user, ts_pass);
		
		// now we post the request for the users on a site
        // now send the new header request
        URL url = new URL(ts_url+"/api/2.0/sites/"+tableau_site+"/projects/"+project_id+"/permissions");
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setRequestProperty("X-Tableau-Auth", tableau_ticket);
        connection.setDoOutput(true);

        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));
        String my_line;
        while ((my_line = in.readLine()) != null) {
        	xml_line = xml_line + my_line;        	
        }
        in.close();    		
		
        //System.out.println(xml_line);
        
        // now lets parse the xml and get the response
        DocumentBuilderFactory xparse = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        
        try {           	
        	db = xparse.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml_line));
            try {
                Document xdoc = db.parse(is);
                xdoc.getDocumentElement().normalize();
            	String user_id = "";
            	String group_id = "";
            	String wb_id = "";
            	String wb_name = "";
            	String cap_name = "";
            	String cap_mode = "";
                
                NodeList nList = xdoc.getElementsByTagName("project");
                int user_count = nList.getLength();
                int mycnt=0;
                int mycnt5=0;
                
                while(mycnt < (user_count))
                {                	
                    Node nNode = nList.item(mycnt);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			String c_name = eElement.getTagName();
            			
            			if(c_name == "project")
            			{
            				wb_id = eElement.getAttribute("id");
            				wb_name = eElement.getAttribute("name");
            			}

                    }                    
                	mycnt++;
                }
                
                NodeList nList2 = xdoc.getElementsByTagName("user");
                int user_count2 = nList2.getLength();
                int mycnt2=0;
                
                while(mycnt2 < (user_count2))
                {
                    Node nNode = nList2.item(mycnt2);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			String c_name = eElement.getTagName();
            			
            			if(c_name == "user")
            			{
            				user_id = eElement.getAttribute("id");
            			}
                    }                    
                	mycnt2++;
                }
                //
                NodeList nList3 = xdoc.getElementsByTagName("group");
                int user_count3 = nList3.getLength();
                int mycnt3=0;
                
                while(mycnt3 < (user_count3))
                {
                    Node nNode = nList3.item(mycnt3);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			String c_name = eElement.getTagName();
            			
            			if(c_name == "group")
            			{
            				group_id = eElement.getAttribute("id");
            			}
                    }                    
                	mycnt3++;
                }

                // 
                NodeList nList4 = xdoc.getElementsByTagName("capability");
                int user_count4 = nList4.getLength();
                int mycnt4=0;
                
                while(mycnt4 < (user_count4))
                {
                    Node nNode = nList4.item(mycnt4);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			String c_name = eElement.getTagName();
            			
            			if(c_name == "capability")
            			{
            				cap_name = eElement.getAttribute("name");
            				cap_mode = eElement.getAttribute("mode");
                			user_full[mycnt5] = wb_id + "," + wb_name + "," + user_id + "," + group_id + "," + cap_name + "," + cap_mode;
                        	mycnt5++;
            			}
                    }                    
                	mycnt4++;
                }

                } catch (SAXException e) {
                // handle SAXException
            } catch (IOException e) {
                // handle IOException
            }
        } catch (ParserConfigurationException e1) {
            // handle ParserConfigurationException
        	System.out.println(e1);
        }        
        
		return user_full;					
	}

	
	
	////////////////////////
	// FOUNDATION
	////////////////////////
	
	// getTsToken
	public String getTsToken(String ts_url, String ts_user) throws Exception {

		// call this function from your app code
        URL url = new URL(ts_url+"/trusted");
        URLConnection connection = url.openConnection();
        connection.setDoOutput(true);

        // prepare the post vars
        OutputStreamWriter out = new OutputStreamWriter(
                                         connection.getOutputStream());
        out.write("username=" + ts_user);
        out.close();      
        
        // parse and get the ticket
        BufferedReader in = new BufferedReader(
        						new InputStreamReader(
        								connection.getInputStream()));

        String tableau_ticket = "";
        String my_line;
        while ((my_line = in.readLine()) != null) {
            tableau_ticket = my_line;        	
        }
        in.close();
        
        // return the ticket
        return tableau_ticket;
	}

	// tsLogin
	public String[] tsLogin() throws Exception {
		String token = "";
		String token_xml = "";
		String xmlString = "";
		String site_id = "";
		String user_id = "";
		String[] token_str = new String[3];
		
		// first make the xml
        try {

            //Creating an empty XML Document
            DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            //Creating the XML tree

            //create the root element and add it to the document
            Element root = doc.createElement("tsRequest");
            doc.appendChild(root);

            //create child element, add an attribute, and add to root
            Element child = doc.createElement("credentials");
            child.setAttribute("name", ts_user);
            child.setAttribute("password", ts_pass);
            root.appendChild(child);

            // last element
            Element ste = doc.createElement("site");
            ste.setAttribute("contentUrl", ts_site);
            child.appendChild(ste);            
            
            //Output the XML to a string
            //set up a transformer
            TransformerFactory transfac = TransformerFactory.newInstance();
            Transformer trans = transfac.newTransformer();
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            trans.setOutputProperty(OutputKeys.INDENT, "yes");

            //create string from xml tree
            StringWriter sw = new StringWriter();
            StreamResult result = new StreamResult(sw);
            DOMSource source = new DOMSource(doc);
            trans.transform(source, result);
            xmlString = sw.toString();
            
            // now send the new header request
            URL url = new URL(ts_url+"/api/2.0/auth/signin");
            URLConnection connection = url.openConnection();
            connection.setDoOutput(true);

            // prepare the post vars
            OutputStreamWriter out = new OutputStreamWriter(
                                             connection.getOutputStream());
            out.write(xmlString);
            out.close();      
            
            // parse and get the ticket
            BufferedReader in = new BufferedReader(
            						new InputStreamReader(
            								connection.getInputStream()));
            String my_line;
            while ((my_line = in.readLine()) != null) {
            	token_xml = token_xml + my_line;        	
            }
            in.close();            

            // now lets parse the xml and get the response
            DocumentBuilderFactory xparse = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = null;
            
            try {           	
            	db = xparse.newDocumentBuilder();
                InputSource is = new InputSource();
                is.setCharacterStream(new StringReader(token_xml));
                try {
                    Document xdoc = db.parse(is);
                    xdoc.getDocumentElement().normalize();
                    
                    NodeList nList = xdoc.getElementsByTagName("credentials");
                    Node nNode = nList.item(0);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement = (Element) nNode;
            			token = eElement.getAttribute("token");
                    }

                    NodeList nList2 = xdoc.getElementsByTagName("site");
                    Node nNode2 = nList2.item(0);
                    if (nNode2.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement2 = (Element) nNode2;
            			site_id = eElement2.getAttribute("id");
                    }
                    
                    NodeList nList3 = xdoc.getElementsByTagName("user");
                    Node nNode3 = nList3.item(0);
                    if (nNode3.getNodeType() == Node.ELEMENT_NODE) {
            			Element eElement3 = (Element) nNode3;
            			user_id = eElement3.getAttribute("id");
                    }
                    
                    
                } catch (SAXException e) {
                    // handle SAXException
                } catch (IOException e) {
                    // handle IOException
                }
            } catch (ParserConfigurationException e1) {
                // handle ParserConfigurationException
            	System.out.println(e1);
            }
            
        } catch (Exception e) {
            System.out.println(e);
        }		

        // setup the var
        token_str[0] = token;
        token_str[1] = site_id;
        token_str[2] = user_id;
        
		return token_str;
	}
	
				
}

