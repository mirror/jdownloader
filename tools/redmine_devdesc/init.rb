require 'redmine'
require 'dispatcher'

Redmine::Plugin.register :redmine_devdesc do
  name 'Developer Description'
  author 'Botzi'
  description 'Allows to add additional information which are only shown for the developer.'
  version '0.0.1'
  author_url 'www.jdownloader.org'
  
  project_module :devdesc do
    permission :devdesc_show, :devdescShow => :show
    permission :devdesc_form, :devdescForm => :form
  end
end

require 'issuePatch'
Dispatcher.to_prepare do
  Issue.send(:include, IssuePatch) unless Issue.included_modules.include? IssuePatch
end

class RedmineDevDescHook < Redmine::Hook::ViewListener
  def view_issues_form_details_bottom(context = { })
    if(User.current.allowed_to?(:devdesc_form, context[:issue].project))
      return "<p><label>#{l(:field_devdesc)}</label><small><textarea id='issue_devdesc' name='issue[devdesc]' cols='60' rows='10' class='wiki-edit'></textarea></small>"
    end
    
    return ''
  end
  
  def view_issues_show_description_bottom(context = { })
    if(User.current.allowed_to?(:devdesc_show, context[:issue].project))
      return "<p><strong>#{l(:field_devdesc)}</strong></p><div class='wiki'>#{textilizable context[:issue].dev_description}</div>"
    end
    
    return ''
  end
  
  def controller_issues_new_before_save(context = { })
    context[:issue].dev_description = context[:params][:issue][:devdesc]
  end
end