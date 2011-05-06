package org.jenkinsci.plugins.kerberos;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.security.SecurityRealm;
import hudson.util.PluginServletFilter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.PrivilegedActionException;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.LoginException;
import javax.servlet.ServletException;

import net.sourceforge.spnego.SpnegoAuthenticator;
import net.sourceforge.spnego.SpnegoHttpFilter.Constants;

import org.acegisecurity.Authentication;
import org.acegisecurity.AuthenticationException;
import org.acegisecurity.AuthenticationManager;
import org.ietf.jgss.GSSException;
import org.kohsuke.stapler.DataBoundConstructor;

public class SSOKerberosSecurityRealm extends SecurityRealm {

	@DataBoundConstructor
	public SSOKerberosSecurityRealm(String kdc, String realm, Boolean overwrite) {
		this.realm = realm;
		this.kdc = kdc;
		this.overwrite = overwrite;

		try {
			setUpKerberos();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		try {

			PluginServletFilter.addFilter(new KerberosAuthenticationFilter());

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private String kdc;
	private String realm;
	private Boolean overwrite;

	@Override
	public SecurityComponents createSecurityComponents() {
		return new SecurityComponents(new AuthenticationManager() {
			public Authentication authenticate(Authentication authentication)
					throws AuthenticationException {
				return authentication;
			}
		});
	}

	private void setUpKerberos() throws LoginException, FileNotFoundException,
			GSSException, PrivilegedActionException, URISyntaxException {

		try {
			createConfigFiles();
			System.setProperty("java.security.krb5.realm", realm);
			System.setProperty("java.security.krb5.kdc", kdc);
			System.setProperty("http.auth.preference", "SSPI");
			System.setProperty("sun.security.krb5.debug", "false");
			System.setProperty("javax.security.auth.useSubjectCredsOnly",
					"false");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void createConfigFiles() throws IOException {
		// Jenkins will write new default kerberos config stuff, if they are not
		// found on JENKINS_HOME

		// The admin have to make sure that useTicketCache=true

		File krbConf = new File(Hudson.getInstance().getRootDir().getPath()
				+ "/krb5.conf");
		if (overwrite && krbConf.exists()) {
			krbConf.delete();
		}

		if (!krbConf.exists()) {
			krbConf.createNewFile();

			FileWriter writer = new FileWriter(krbConf);
			writer.write("[libdefaults]\n");
			writer.write("default_tkt_enctypes = DES-CBC-CRC\n");
			writer.write("default_tgs_enctypes = DES-CBC-CRC\n");
			writer.write("permitted_enctypes = DES-CBC-CRC\n");
			writer.write("udp_preference_limit = 1\n");
			writer.write("[appdefaults]\n");
			writer.write("forwardable = true");
			writer.flush();
			writer.close();
		}

		File jaasConf = new File(Hudson.getInstance().getRootDir().getPath()
				+ "/jaas.conf");

		if (overwrite && jaasConf.exists()) {
			jaasConf.delete();
		}

		if (!jaasConf.exists()) {
			jaasConf.createNewFile();

			FileWriter writer = new FileWriter(jaasConf);
			writer.write("Kerberos {\n");
			writer.write("     com.sun.security.auth.module.Krb5LoginModule required\n");
			writer.write(" doNotPrompt=false useTicketCache=true useKeyTab=false;\n");
			writer.write("};");

			writer.write("spnego-client {");
			writer.write("	com.sun.security.auth.module.Krb5LoginModule required;\n");
			writer.write("}\n;");
			writer.write("spnego-server {\n");
			writer.write("  com.sun.security.auth.module.Krb5LoginModule required storeKey=true isInitiator=false useKeyTab=false;\n");
			writer.write("};\n");

			writer.write("com.sun.security.jgss.initiate {\n");
			writer.write(" com.sun.security.auth.module.Krb5LoginModule required\n");
			writer.write(" doNotPrompt=true\n");
			writer.write(" storeKey=true;\n");
			writer.write("};");
			writer.write("com.sun.security.jgss.accept {\n");
			writer.write(" com.sun.security.auth.module.Krb5LoginModule required\n");
			writer.write(" useKeyTab=false\n");
			writer.write(" storeKey=true;\n");

			writer.write("};\n");

			writer.flush();
			writer.close();
		}

	}

	public String getKdc() {
		return kdc;
	}

	public void setKdc(String kdc) {
		this.kdc = kdc;
	}

	public String getRealm() {
		return realm;
	}

	public void setRealm(String realm) {
		this.realm = realm;
	}

	public Boolean getOverwrite() {
		return overwrite;
	}

	public void setOverwrite(Boolean overwrite) {
		this.overwrite = overwrite;
	}

	@Extension
	public static class DescriptorImpl extends Descriptor<SecurityRealm> {

		@Override
		public String getDisplayName() {
			return "Kerberos SSO";
		}

	}

}
