package com.example;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.google.gson.Gson;

/**
 * @author novas
 */
@Component
public class TestJson {
	@PostConstruct
	public void generateJson() {
			Gson gson = new Gson();
			SimpleDateFormat sdf = new SimpleDateFormat("HHmmss");
			
			// contoh request yang dikirim.
			Login bean = new Login();
			bean.setIp("192.168.1.56");
			bean.setUserId("user");
			bean.setPassword("password");
			bean.setPin("1234");
			bean.setTerminalId("novas");
			bean.setRequestTime(sdf.format(new Date()));
			
			// semua object request disimpan dalam array list.
			List<String> list = new ArrayList<>();
			list.add(gson.toJson(bean));
			
			// setiap data yang dikirim harus di wrapper/bungkus, 
			// dimana property type mewakili jenis request yang diminta.
			Wrapper wrap = new Wrapper();
			wrap.setId("ssit01@novas");
			wrap.setType("LOGIN");
			wrap.setData(list);
			
			String data = gson.toJson(wrap);
			
			NioClient conn = new NioClient("ipserver", 9060);
			conn.send(data);
			conn.start();
	}
	class Wrapper {
		private String type;
		private String id;
		private List<String> data;
		
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		public List<String> getData() {
			return data;
		}
		public void setData(List<String> data) {
			this.data = data;
		}
	}
	class Login {
		private String userId;
		private String password;
		private String pin;
		private String terminalId;
		private String ip;
		private String requestTime;
		
		public String getUserId() {
			return userId;
		}
		public void setUserId(String userId) {
			this.userId = userId;
		}
		public String getPassword() {
			return password;
		}
		public void setPassword(String password) {
			this.password = password;
		}
		public String getPin() {
			return pin;
		}
		public void setPin(String pin) {
			this.pin = pin;
		}
		public String getTerminalId() {
			return terminalId;
		}
		public void setTerminalId(String terminalId) {
			this.terminalId = terminalId;
		}
		public String getIp() {
			return ip;
		}
		public void setIp(String ip) {
			this.ip = ip;
		}
		public String getRequestTime() {
			return requestTime;
		}
		public void setRequestTime(String requestTime) {
			this.requestTime = requestTime;
		}
	}
}
