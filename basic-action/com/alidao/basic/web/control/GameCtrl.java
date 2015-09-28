package com.alidao.basic.web.control;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alidao.basic.entity.Game;
import com.alidao.basic.entity.Product;
import com.alidao.basic.service.GameService;
import com.alidao.basic.service.ProductService;
import com.alidao.jse.util.StringUtil;
import com.alidao.jxe.control.WebCtrl;
import com.alidao.jxe.model.Condition;
import com.alidao.jxe.model.PageParam;
import com.alidao.jxe.util.HttpUtil;

@Controller
@RequestMapping("game")
public class GameCtrl extends WebCtrl {

	@Autowired
	private GameService gameService;
	
	@Autowired
	private ProductService productService;
	
	@RequestMapping("init")
	public void init(HttpServletRequest request, Model model) {
		model.addAttribute("webapp", HttpUtil.getWebAppUrl(request));
	}

	@RequestMapping("page")
	public void page(Game object,  
			PageParam pageParam, HttpServletResponse response) throws Exception {
		gameService.page(pageParam, object).jsonOut(response);
	}


	@RequestMapping("input")
	public void edit(String id, Model model) {
		List<Product> list = productService.list(null);
		List<Product> result = new ArrayList<Product>();
		for (int i = 0; list != null && i < list.size(); i++) {
			Game object = new Game();
			object.addCondition("product_id", 
					Condition.CDT_LIKE, ("'%" + list.get(i).getId() + "%'"), 
					Condition.SEP_AND);
			object = gameService.find(object);
			if (object == null) {
				result.add(list.get(i));
			}
		}
		model.addAttribute("productList", result);
	}

	@RequestMapping("save")
	public void save(
			HttpServletResponse response, 
			Game object)
			throws Exception {
		if (StringUtil.isEmpty(object.getId())) {
			getResponse(
				gameService.save(object)
			).jsonOut(response);
		} else {
			getResponse(
				gameService.mdfy(object)
			).jsonOut(response);
		}
	}
	
	@RequestMapping("lose/{id}")
	public void lose(
			@PathVariable("id") String id, 
			HttpServletResponse response)
			throws Exception {
		getResponse(
			gameService.lose(id)
		).jsonOut(response);
	}
	
}
