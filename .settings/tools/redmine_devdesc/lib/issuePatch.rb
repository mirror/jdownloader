require_dependency 'issue'

module IssuePatch
  def self.included(base) # :nodoc:
    base.extend(ClassMethods)

    base.send(:include, InstanceMethods)

    # Same as typing in the class 
    base.class_eval do
      acts_as_searchable :columns => ["#{table_name}.dev_description"]
    end
  end
  
  module ClassMethods
  end
  
  module InstanceMethods
  end    
end 