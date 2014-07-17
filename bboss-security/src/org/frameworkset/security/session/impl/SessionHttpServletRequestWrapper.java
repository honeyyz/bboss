/*
 *  Copyright 2008 bbossgroups
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.frameworkset.security.session.impl;

import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.frameworkset.security.session.Session;
import org.frameworkset.security.session.SessionBasicInfo;
import org.frameworkset.security.session.domain.App;
import org.frameworkset.security.session.domain.CrossDomain;

import com.frameworkset.util.StringUtil;

/**
 * <p>Title: SessionHttpServletRequestWrapper.java</p> 
 * <p>Description: </p>
 * <p>bboss workgroup</p>
 * <p>Copyright (c) 2008</p>
 * @Date 2014年4月30日
 * @author biaoping.yin
 * @version 3.8.0
 */
public class SessionHttpServletRequestWrapper extends HttpServletRequestWrapper {
	private String sessionid;
	private HttpSessionImpl session;
	private HttpServletResponse response;
	private ServletContext servletContext;	
	public SessionHttpServletRequestWrapper(HttpServletRequest request,HttpServletResponse response,ServletContext servletContext) {
		super(request);
		sessionid = StringUtil.getCookieValue((HttpServletRequest)request, SessionHelper.getSessionManager().getCookiename());
		this.servletContext = servletContext;
		this.response = response;
	}

	@Override
	public HttpSession getSession() {
		
		 return getSession(true);
	}

	private String getAppKey()
	{
		String appcode = SessionHelper.getSessionManager().getAppcode();
		if(appcode != null)
		{
			return appcode;
		}
		String contextpath = this.getContextPath().replace("/", "");
		if(contextpath.equals(""))
			contextpath = "ROOT";
		return contextpath;
	}
	@Override
	public HttpSession getSession(boolean create) {
		if( SessionHelper.getSessionManager().usewebsession())
		{
			// TODO Auto-generated method stub
			return super.getSession();
		}
		if(sessionid == null)
		{
			if(create)
			{

				String contextpath = getAppKey();
				SessionBasicInfo sessionBasicInfo = new SessionBasicInfo();
				sessionBasicInfo.setAppKey(contextpath);
				sessionBasicInfo.setReferip(StringUtil.getClientIP(this));
				sessionBasicInfo.setRequesturi(this.getRequestURI());
				
				Session session = SessionHelper.createSession(sessionBasicInfo);				
				sessionid = session.getId();
				this.session = new HttpSessionImpl(session,servletContext);

				writeCookies( );
				return this.session;
			}
			else
			{
				return null;
			}
		}
		else if(session != null)
		{
			return session;
		}
		else
		{
			String contextpath = getAppKey();

			Session session = SessionHelper.getSession(contextpath,sessionid);
			if(session == null)//session不存在，创建新的session
			{				
				if(create)
				{

				
					
					SessionBasicInfo sessionBasicInfo = new SessionBasicInfo();
					sessionBasicInfo.setAppKey(contextpath);
					sessionBasicInfo.setReferip(StringUtil.getClientIP(this));
					sessionBasicInfo.setRequesturi(this.getRequestURI());
					
					session = SessionHelper.createSession(sessionBasicInfo);
					sessionid = session.getId();
					this.session =  new HttpSessionImpl(session,servletContext);

					writeCookies( );
				}
			}
			else
			{
				this.session =  new HttpSessionImpl(session,servletContext);
			}
			return this.session;
		}
		
		
	}

	public void touch() {
		if( SessionHelper.getSessionManager().usewebsession())
			return;
		if(this.sessionid != null )
		{
			if(session == null)
			{
				String contextpath = getAppKey();
				Session session_ = SessionHelper.getSession(contextpath, sessionid);
				if(session_ == null || !session_.isValidate())
					return;
				this.session =  new HttpSessionImpl(session_,servletContext);
			}
			if(session != null && !session.isNew() )
			{
				session.touch(this.getRequestURI());
			}
		}
		
	}
	private void writeCookies( )
	{
		int cookielivetime = -1;
		CrossDomain crossDomain = SessionHelper.getSessionManager().getCrossDomain() ;
		if(SessionHelper.getSessionManager().getCrossDomain() == null)
		{
			StringUtil.addCookieValue(this, response, SessionHelper.getSessionManager().getCookiename(), sessionid, cookielivetime,SessionHelper.getSessionManager().isHttpOnly(),
					SessionHelper.getSessionManager().isSecure(),SessionHelper.getSessionManager().getDomain());
		}
		else
		{
			List<App> apps = crossDomain.getApps();
			if(crossDomain.get_paths() != null)
			{
				for(String path:crossDomain.get_paths())
				{
					StringUtil.addCookieValue(this, path,
												response, 
												SessionHelper.getSessionManager().getCookiename(), 
												sessionid, cookielivetime,
												SessionHelper.getSessionManager().isHttpOnly(),								
												SessionHelper.getSessionManager().isSecure(),
												crossDomain.getDomain());
				}
			}
			else
			{
				for(App app:apps)
				{
					if(app.getPath() == null)
					{
						StringUtil.addCookieValue(this, response, SessionHelper.getSessionManager().getCookiename(), sessionid, cookielivetime,SessionHelper.getSessionManager().isHttpOnly(),
								SessionHelper.getSessionManager().isSecure(),crossDomain.getDomain());
					}
					else
					{
						
						StringUtil.addCookieValue(this, app.getPath(),response, SessionHelper.getSessionManager().getCookiename(), sessionid, cookielivetime,SessionHelper.getSessionManager().isHttpOnly(),								
								SessionHelper.getSessionManager().isSecure(),crossDomain.getDomain());
						
						
					}
				}
			}
			
		}
	}

}
