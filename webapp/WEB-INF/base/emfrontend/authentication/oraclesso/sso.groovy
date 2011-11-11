import com.entermedia.sso.OracleSSO

OracleSSO sso = new OracleSSO();
sso.setModuleManager(moduleManager);
sso.setUserManager(userManager);
sso.oracleSsoLogin(context);