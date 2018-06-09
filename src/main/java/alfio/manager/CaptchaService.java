package alfio.manager;

import javax.servlet.http.HttpServletRequest;

public interface CaptchaService {
	public boolean checkCaptcha(HttpServletRequest req);
}
