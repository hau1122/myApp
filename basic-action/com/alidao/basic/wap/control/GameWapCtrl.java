package com.alidao.basic.wap.control;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alidao.basic.entity.FriendGameRecord;
import com.alidao.basic.entity.Game;
import com.alidao.basic.entity.GameRecord;
import com.alidao.basic.service.FriendGameRecordService;
import com.alidao.basic.service.GameRecordService;
import com.alidao.basic.service.GameService;
import com.alidao.common.Constants;
import com.alidao.jse.util.StringUtil;
import com.alidao.jxe.control.WebCtrl;
import com.alidao.jxe.help.PowerHelper;
import com.alidao.jxe.model.Condition;
import com.alidao.jxe.model.ResponseForAjax;
import com.alidao.jxe.util.HttpUtil;
import com.alidao.users.authorizing.UseridTracker;
import com.alidao.users.entity.User;
import com.alidao.users.service.UserService;
import com.alidao.wxapi.bean.TokenForWxapis;
import com.alidao.wxapi.bean.UserForWxUnion;
import com.alidao.wxapi.util.OpenidTracker;
import com.alidao.wxapi.util.WxapiUtil;
@Controller
@RequestMapping("wap/game")
public class GameWapCtrl extends WebCtrl {
	
	@Autowired
	private GameService gameService;
	
	@Autowired
	private FriendGameRecordService friendGameRecordService;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private GameRecordService gameRecordService;
	
	/**
	 * 游戏页面
	 * @throws Exception 
	 * 
	 */
	@RequestMapping("index")
	public void index(HttpServletRequest request,
			String id,Model model) throws Exception {
		String sn="";
		if(!StringUtil.isEmpty(id)&&id.length()>32){
			sn=id.substring(32);
			id=id.substring(0,32);
		}
		String openId=OpenidTracker.get();
		Date date=new Date();
		Game game=gameService.find(id);
		String appId = Constants.get("wxapi.appid");
		String appsecret = Constants.get("wxapi.appsecret");
		UserForWxUnion union = getUserForWxUnion(appId, appsecret, openId);
		String userId = isSecurity(request) ? UseridTracker.get() : null;
		boolean submitFlag=true;//是否可提交分数
		Integer maxScore=0;//用户提交的玩游戏最高分
		boolean buyer=false;//是否为购买产品用户
		if(!StringUtil.isEmpty(userId)){
			if(StringUtil.isEmpty(sn)){
				GameRecord gameRecord=new GameRecord();
				gameRecord.setGameId(id);
				gameRecord.setUserId(userId);
				gameRecord=gameRecordService.find(gameRecord);
				if(gameRecord!=null){
					buyer=true;
					if(StringUtil.isEmpty(sn)){
						sn=gameRecord.getShareSn();
					}
					maxScore=gameRecord.getScore();//购买产品用户的最高分
				}else{
					if(gameService.game(userId, id)&&date.after(game.getStartTime())&&date.before(game.getEndTime())){
						buyer=true;
						GameRecord record=new GameRecord();
						record.setGameId(id);
						record.setUserId(userId);
						record=gameRecordService.find(record);
						if(record==null){
							record=gameService.addBuyerGameRecord(id, userId, openId, 0);
						}
						sn=record.getShareSn();
					}
					submitFlag=false;
				}
			}else{
				GameRecord gameRecord=new GameRecord();
				gameRecord.setGameId(id);
				gameRecord.setShareSn(sn);
				gameRecord=gameRecordService.find(gameRecord);
				if(gameRecord!=null&&gameRecord.getUserId().equals(userId)){
					buyer=true;
					maxScore=gameRecord.getScore();//购买产品用户的最高分
				}else{
					FriendGameRecord friendRecord=new FriendGameRecord();
					friendRecord.setOpenid(openId);
					friendRecord.setShareSn(sn);
					friendRecord=friendGameRecordService.find(friendRecord);
					if(friendRecord!=null){
						submitFlag=false;
					}
					maxScore=getFriendGameRecordMaxScore(openId);
				}
			}
		}else{
			if(!StringUtil.isEmpty(sn)){
				FriendGameRecord friendRecord=new FriendGameRecord();
				friendRecord.setOpenid(openId);
				friendRecord.setShareSn(sn);
				friendRecord=friendGameRecordService.find(friendRecord);
				if(friendRecord!=null){
					submitFlag=false;
				}
				maxScore=getFriendGameRecordMaxScore(openId);
			}else{
				submitFlag=false;
				maxScore=getFriendGameRecordMaxScore(openId);
			}
		}
		String lctx = HttpUtil.getWebAppUrl(request);
		model.addAttribute("lctx", lctx);
		model.addAttribute("id", id);
		model.addAttribute("shareSn", sn);
		model.addAttribute("submitFlag", submitFlag);
		model.addAttribute("userId", userId);
		model.addAttribute("maxScore", maxScore);
		model.addAttribute("buyer", buyer);
		model.addAttribute("headImage", union.getHeadimgurl());
		model.addAttribute("nickName", !StringUtil.isEmpty(union.getNickname())?union.getNickname():"");
	}
	
	public int getFriendGameRecordMaxScore(String openId){
		FriendGameRecord friendGameRecord=new FriendGameRecord();
		friendGameRecord.setOpenid(openId);
		friendGameRecord.addOrderBy("max_score", true);
		friendGameRecord=friendGameRecordService.find(friendGameRecord);
		return friendGameRecord!=null?friendGameRecord.getMaxScore():0;
	}
	/**
	 * 提交分数
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	@RequestMapping("submitScore")
	public void submitScore(HttpServletRequest request,
			HttpServletResponse response,String userId,String id,String sn,Integer score) throws IOException {
		ResponseForAjax resp = new ResponseForAjax();
		Game game=gameService.find(id);
		Date date=new Date();
		int result=-3;//不在有效时间
		if(date.after(game.getStartTime())&&date.before(game.getEndTime())){
			result=gameService.submitScore(userId,id,sn,score);
		}
		if (result == -3) {
			if (date.before(game.getStartTime())) {
				resp.setMessage("活动还没有开始哟~");
			} else {
				resp.setMessage("游戏时间已截止，感谢你的友情参与咯！");
			}
		} else if (result == -2) {
			resp.setMessage("您已经给TA加油过咯，重复无效哦!");
		} else if (result == 0) {
			resp.setMessage("对不起，您不能提交该分数!");
		}
		resp.setResult(result);
		resp.jsonOut(response);
	}
	
	/**
	 * 排行榜
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	@RequestMapping("charts")
	public void charts(HttpServletRequest request,String id,String userId,String sn,Model model) throws IOException {
		String openId=OpenidTracker.get();
		GameRecord object=getBuyer(id, sn, userId, openId);
		List<GameRecord> gameRecordList=income(id,object);
		model.addAttribute("object", object);
		model.addAttribute("gameRecordList", gameRecordList);
		model.addAttribute("userId", userId);
		model.addAttribute("id", id);
		model.addAttribute("shareSn", sn);
		model.addAttribute("lctx", HttpUtil.getWebAppUrl(request));
	}
	
	/**
	 * 亲友团
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	@RequestMapping("friend")
	public void friend(HttpServletRequest request,String id,String userId,String sn,Model model) throws IOException {
		String openId=OpenidTracker.get();
		GameRecord object=getBuyer(id, sn, userId, openId);
		FriendGameRecord friendGameRecord=new FriendGameRecord();
		List<FriendGameRecord> friendRecordList=new ArrayList<FriendGameRecord>();
		if(object!=null&&!StringUtil.isEmpty(object.getShareSn())){
			friendGameRecord.setShareSn(object.getShareSn());
			friendRecordList=friendGameRecordService.list(friendGameRecord);
		}
		income(id,object);
		model.addAttribute("object", object);
		model.addAttribute("friendRecordList", friendRecordList);
		model.addAttribute("lctx", HttpUtil.getWebAppUrl(request));
		model.addAttribute("id", id);
		model.addAttribute("shareSn", sn);
	}
	
	/**
	 * 提交成功页面
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	@RequestMapping("success")
	public void success(HttpServletRequest request,String id,String sn,Integer score,Model model) throws IOException {
		GameRecord object=new GameRecord();
		object.setShareSn(sn);
		object=gameRecordService.find(object);
		model.addAttribute("object", object);
		model.addAttribute("lctx", HttpUtil.getWebAppUrl(request));
		model.addAttribute("id", id);
		model.addAttribute("shareSn", sn);
		model.addAttribute("totalScore", object.getScore()+object.getFriendScore());
		model.addAttribute("score", score);
	}
	
	/**获取购买产品者
	 * @throws Exception **/
	public GameRecord getBuyer(String gameId,String sn,String userId,String openId){
		GameRecord object=new GameRecord();
		object.setGameId(gameId);
		if(StringUtil.isEmpty(sn)){
			if(!StringUtil.isEmpty(userId)){
				object.setUserId(userId);
				object=gameRecordService.find(object);
			}else{
				FriendGameRecord friendRecord=new FriendGameRecord();
				friendRecord.setOpenid(openId);
				friendRecord.addCondition("share_sn <> '' ", Condition.SEP_AND);
				friendRecord=friendGameRecordService.find(friendRecord);
				if(friendRecord!=null){
					object.setShareSn(friendRecord.getShareSn());
					object=gameRecordService.find(object);
				}else{
					object=null;
				}
			}
		}else{
			object.setShareSn(sn);
			object=gameRecordService.find(object);
		}
		return object;
	}
	/**计算排名和收益率**/
	public List<GameRecord> income(String gameId, GameRecord object) {
		GameRecord gameRecord=new GameRecord();
		gameRecord.addOrderBy("score+friendScore",true);
		gameRecord.setGameId(gameId);
		List<GameRecord> gameRecordList=gameRecordService.list(gameRecord);
		NumberFormat df=NumberFormat.getNumberInstance() ;
		df.setMaximumFractionDigits(2);
		if (gameRecordList != null && gameRecordList.size() > 0) {
			if (gameRecordList.size() == 1) {
				gameRecordList.get(0).setIncome(20.00);
				gameRecordList.get(0).setRanking(1);
				object.setRanking(gameRecordList.get(0).getRanking());
				object.setIncome(gameRecordList.get(0).getIncome());
			} else {
				for (int i = 0; i < gameRecordList.size(); i++) {
					Double step = ((double) (20-10)) / (gameRecordList.size() - 1);
					Double income = 20.00 - i * step;
					GameRecord record = gameRecordList.get(i);
					record.setIncome(Double.valueOf(df.format(income)));
					
					record.setRanking(i + 1);
					if (object != null && !StringUtil.isEmpty(object.getUserId()) 
							&& object.getUserId().equals(record.getUserId())) {
						object.setRanking(record.getRanking());
						object.setIncome(record.getIncome());
					}
				}
			}
		}
		return gameRecordList;
	}
	
	public UserForWxUnion getUserForWxUnion(String appId,String appsecret,String openId){
		TokenForWxapis token;
		UserForWxUnion union = new UserForWxUnion();
		try {
			token = WxapiUtil.
					getWxapisToken(appId, appsecret);
			String access_token = token.getAccess_token();
			union = WxapiUtil.
					getWxUnionUser(access_token, openId);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return union;
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
