package com.alidao.basic.wap.control;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import com.alidao.basic.service.SigninRecordService;
import com.alidao.jse.util.StringUtil;
import com.alidao.jxe.control.WebCtrl;
import com.alidao.jxe.help.PowerHelper;
import com.alidao.jxe.model.ResponseForAjax;
import com.alidao.jxe.util.HttpUtil;
import com.alidao.users.authorizing.UseridTracker;
import com.alidao.users.entity.User;
import com.alidao.users.service.UserService;
import com.alidao.wxapi.util.OpenidTracker;
import com.alidao.wxapi.util.WxapiUtil;

@Controller
@RequestMapping("wap/signin")
public class SigninWapCtrl extends WebCtrl {
	
	@Autowired
	private SigninRecordService signinRecordService;
	
	@Autowired
	private UserService userService;

	@RequestMapping("index")
	public void index(Model model, HttpServletRequest request) throws IOException {
		model.addAttribute("lctx", HttpUtil.getWebAppUrl(request));
		String _user = isSecurity(request) ? UseridTracker.get() : null;
		model.addAttribute("proverb", signinRecordService.findProverb().getContent());
		model.addAttribute("status", signinRecordService.thisWeekSigninRecord(_user));
		model.addAttribute("userId", _user);
	}

	/**
	 * 签到
	 * 
	 * @throws IOException
	 */
	@RequestMapping("signin")
	public void signin(HttpServletResponse response, String userId) throws IOException {
		ResponseForAjax resp = new ResponseForAjax();
		try {
			Map<String, Object> map = signinRecordService.signin(userId);
			if (map.get("result") == Integer.valueOf(1)) {
				resp.setResult(1);
				resp.setData(map.get("integral"));
			} else if (map.get("result") == Integer.valueOf(-1)) {
				resp.setResult(-1);
			} else if (map.get("result") == Integer.valueOf(-2)) {
				resp.setResult(-2);
			} else if (map.get("result") == Integer.valueOf(0)) {
				resp.setResult(0);
			}
		} catch (Exception e) {
			resp = getErrResponse(e);
		}
		resp.jsonOut(response);
	}
	
	/**
	 * 判断是否安全
	 * @param request
	 * @return
	 */
	private Boolean isSecurity(
			HttpServletRequest request) {
		if (WxapiUtil.fromMicBrowser(request)) {
			return hasBinding(request);
		} else {
			return hasLogined(request);
		}
	}
	
	/**
	 * 判断微信用户是否绑定
	 * @param request 
	 * @return
	 */
	private boolean hasBinding(HttpServletRequest request) {
		String openid = OpenidTracker.get();
		User user = userService.find(openid, User.BINDED_WECHAT);
		if (user != null && StringUtil.isNotBlank(user.getSessionKey())) {
			UseridTracker.set(user.getId());
			request.setAttribute("uid", user.getId());
			request.setAttribute("u.status", user.getStatus());
			return true;
		}
		return false;
	}
	
	/**
	 * 判断用户的会话时否已经丢失或失效
	 * @param request 
	 * @return
	 */
	private boolean hasLogined(HttpServletRequest request) {
		String power = PowerHelper.get();
		if (StringUtil.isNotBlank(power)) {
			User user = userService.find(power);
			if (user != null) {
				UseridTracker.set(power);
				request.setAttribute("uid", power);
				int status = user.getStatus();
				request.setAttribute("u.status", status);
				return true;
			}
		}
		return false;
	}
	
}
