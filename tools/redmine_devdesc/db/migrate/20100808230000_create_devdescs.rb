class CreateDevdescs < ActiveRecord::Migration
  def self.up
    add_column :issues, :dev_description, :string
  end

  def self.down
    remove_column :issues, :dev_description
  end
end
