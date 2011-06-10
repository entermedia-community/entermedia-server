package org.entermedia.email;

import org.openedit.data.Searcher;

public interface EmailSearcher extends Searcher{

	public void setEmailArchive(EmailArchive inEmailArchive);
	public EmailArchive getEmailArchive();
}
