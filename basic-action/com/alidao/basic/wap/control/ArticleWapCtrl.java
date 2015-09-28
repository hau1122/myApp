package com.alidao.basic.wap.control;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alidao.basic.entity.Article;
import com.alidao.basic.service.ArticleService;

@Controller
@RequestMapping("wap/article")
public class ArticleWapCtrl {

	@Autowired
	private ArticleService articleService;
	
	@RequestMapping("html/{id}")
	public String html(
			@PathVariable("id") Long id,
			Model model, HttpServletRequest request) {
		Article object = articleService.find(id);
		model.addAttribute("object", object);
		return "wap/article/html";
	}
	
}
