local folder=Instance.new("Folder")
folder.Name="SmokeFolder"
folder.Parent=workspace
local part=Instance.new("Part")
part.Name="SmokePart"
part.Position=Vector3.new(1,2,3)
part.Parent=folder
part:SetAttribute("Build",9)
assert(part:IsA("BasePart"))
assert(folder:FindFirstChild("SmokePart")==part)
assert(part:GetAttribute("Build")==9)
assert(part:GetFullName()=="Workspace.SmokeFolder.SmokePart")
assert(Vector3.new(3,4,0).Magnitude==5)
assert(game:GetService("RunService"):IsStudio())
assert(game:GetService("Workspace")==workspace)
local fired=false
folder.ChildAdded:Connect(function(child) fired=child.Name=="AddedChild" end)
local child=Instance.new("Folder")
child.Name="AddedChild"
child.Parent=folder
assert(fired)
print("ROBLOX_COMPAT_SMOKE_OK",part:GetFullName())
