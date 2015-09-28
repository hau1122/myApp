package com.alidao.basic.wap.control;

import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.alidao.basic.entity.Option;
import com.alidao.basic.entity.Questionnaire;
import com.alidao.basic.entity.QuestionnaireRecord;
import com.alidao.basic.entity.Topic;
import com.alidao.basic.service.OptionService;
import com.alidao.basic.service.QuestionnaireRecordService;
import com.alidao.basic.service.QuestionnaireService;
import com.alidao.basic.service.TopicService;
import com.alidao.jxe.control.WebCtrl;
import com.alidao.jxe.model.ResponseForAjax;
import com.alidao.users.authorizing.UseridTracker;

@Controller
@RequestMapping("wap/questionnaire")
public class QuestionnaireWapCtrl extends WebCtrl {
	@Autowired
	private TopicService topicService;

	@Autowired
	private OptionService optionService;

	@Autowired
	private QuestionnaireService questionnaireService;

	@Autowired
	private QuestionnaireRecordService questionnaireRecordService;
	
	@RequestMapping("gater")
	public String gater(Model model) {
		Questionnaire questionnaire = new Questionnaire();
		questionnaire.addOrderBy("create_time", true);
		questionnaire = questionnaireService.find(questionnaire);
		String redirect_url = "/wap/questionnaire/index?id=";
		if (questionnaire != null) {
			redirect_url += questionnaire.getId();
		}
		return "redirect:".concat(redirect_url);
	}

	/**
	 * 问卷页面
	 * 
	 */
	@RequestMapping("index")
	public ModelAndView index(Model model, String id) {
		ModelAndView modelAndView = new ModelAndView();
		QuestionnaireRecord questionnaireRecord = new QuestionnaireRecord();
		questionnaireRecord.setUserId(UseridTracker.get());
		questionnaireRecord.setQuestionnaireId(id);
		questionnaireRecord = questionnaireRecordService.find(questionnaireRecord);
		if (questionnaireRecord == null) {
			Questionnaire questionnaire = questionnaireService.find(id);
			if (questionnaire != null) {
				Topic topic = new Topic();
				topic.addOrderBy("seq");
				topic.setQuestionnaireId(questionnaire.getId());
				List<Topic> list = topicService.list(topic);
				Option option = new Option();
				for (Topic object : list) {
					option.setTopicId(object.getId());
					option.addOrderBy("seq");
					object.setOptionList(optionService.list(option));
				}
				model.addAttribute("integral", questionnaire.getIntegral());
				model.addAttribute("questionnaireId", questionnaire.getId());
				model.addAttribute("topicList", list);
				modelAndView.setViewName("wap/questionnaire/index");
			}
		} else {
			modelAndView.setViewName("wap/questionnaire/finish");
		}

		return modelAndView;
	}

	@RequestMapping("save")
	public void save(HttpServletResponse response, String questionnaireId,
			String[] topicId, String[] optionId) throws IOException {
		ResponseForAjax resp = new ResponseForAjax();
		try {
			int result = questionnaireRecordService.save(questionnaireId,
					topicId, optionId);
			resp = getResponse(result);
			if (result == -1) {
				resp.setResult(-1);
				resp.setMessage("你已参与该问卷调查，<br>请勿重复操作!");
			}
		} catch (Exception e) {
			resp = getErrResponse(e);
		}
		resp.jsonOut(response);
	}

	@RequestMapping("finish")
	public void finish(Model model, Integer integral) {
		model.addAttribute("integral", integral);
	}
	
}
