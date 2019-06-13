#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html
#
Pod::Spec.new do |s|
  s.name             = 'nim'
  s.version          = '0.0.1'
  s.summary          = 'A new flutter plugin project.'
  s.description      = <<-DESC
A new flutter plugin project.
                       DESC
  s.homepage         = 'http://example.com'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'Your Company' => 'email@example.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.public_header_files = 'Classes/**/*.h'
  s.resources = 'Classes/Resources/*.*'
  s.dependency 'Flutter'
  s.dependency 'NIMSDK', '~> 6.5.5'
   s.dependency 'SVProgressHUD'
  s.dependency 'SDWebImage', '~> 4.4.6'
  s.dependency 'Toast', '~> 3.0'
  s.dependency 'M80AttributedLabel', '~> 1.6.3'
  s.dependency 'TZImagePickerController', '~> 3.0.7'
  s.ios.deployment_target = '8.0'
end

