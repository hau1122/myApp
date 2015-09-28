package com.alidao.basic.web.control;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alidao.basic.entity.Article;
import com.alidao.basic.service.ArticleService;
import com.alidao.jxe.control.WebCtrl;
import com.alidao.jxe.model.PageParam;
import com.alidao.jxe.util.HttpUtil;

@Controller
@RequestMapping("article")
public class ArticleCtrl extends WebCtrl {

	@Autowired
	private ArticleService articleService;

	@RequestMapping("init")
	public void init(
			HttpServletRequest request, 
			Model model) {
		String ctx = HttpUtil.getWebAppUrl(request);
		String url = "/wap/article/html/";
		model.addAttribute("url", ctx + url);
	}

	@RequestMapping("page")
	public void page(
			PageParam pageParam, 
			Article object,
			HttpServletResponse response) 
					throws Exception {
		articleService.page(
			pageParam, object
		).jsonOut(response);
	}

	@RequestMapping("list")
	public void list(
			Article object, 
			HttpServletResponse response)
			throws Exception {
		getQueryResponse(
			articleService.list(object)
		).jsonOut(response);
	}

	@RequestMapping("edit")
	public void edit(
			Long id, 
			Model model) {
		if (id != null) {
			Article object = articleService.find(id);
			model.addAttribute("object", object);
		}
	}

	@RequestMapping("save")
	public void save(
			HttpServletResponse response, 
			Article object)
			throws Exception {
		if (object.getId() == null) {
			getResponse(
				articleService.save(object)
			).jsonOut(response);
		} else {
			getResponse(
				articleService.mdfy(object)
			).jsonOut(response);
		}
	}

	@RequestMapping("lose/{id}")
	public void lose(
			@PathVariable("id") Long id, 
			HttpServletResponse response)
			throws Exception {
		getResponse(
			articleService.lose(id)
		).jsonOut(response);
	}

}
