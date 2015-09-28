package com.alidao.basic.wap.control;

import java.util.Random;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alidao.common.Constants;
import com.alidao.jse.util.URLDecoder;
import com.alidao.jxe.util.HttpUtil;

@Controller
@RequestMapping("wap/share")
public class ShareWapCtrl {

	@RequestMapping("principal")
	public void principal(HttpServletRequest request, Model model) {
		model.addAttribute("lctx", HttpUtil.getWebAppUrl(request));
		model.addAttribute("spid", Constants.get("product.special.id"));
	}
	
	@RequestMapping("qixi/1")
	public void qixi1(HttpServletRequest request, Model model) {
		model.addAttribute("lctx", HttpUtil.getWebAppUrl(request));
	}
	
	@RequestMapping("qixi/2")
	public void qixi2(HttpServletRequest request, 
			String name, String gender, Model model) {
		model.addAttribute("lctx", HttpUtil.getWebAppUrl(request));
		model.addAttribute("name", URLDecoder.decode(name));
		model.addAttribute("gender", gender);
		Integer img_no_max = null;
		if ("girl".equals(gender)) { 
			img_no_max = 3; 
			model.addAttribute("imageUrl", "girl_"+(new Random().nextInt(img_no_max) + 1)+".png");
		} else { img_no_max = 4; 
			model.addAttribute("imageUrl", "boy_"+(new Random().nextInt(img_no_max) + 1)+".png");
		}
	}
	
	@RequestMapping("qixi/3")
	public void qixi3(HttpServletRequest request, String gender, Model model) {
		model.addAttribute("lctx", HttpUtil.getWebAppUrl(request));
		model.addAttribute("spid", Constants.get("product.qixi.id"));
		model.addAttribute("gender", gender);
	}
	
}
