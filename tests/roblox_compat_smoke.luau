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

local rayHit=workspace:Raycast(Vector3.new(1,2,10),Vector3.new(0,0,-20))
assert(rayHit and rayHit.Instance==part)
assert(math.abs(rayHit.Distance-5)<0.00001)
local radiusHits=workspace:GetPartBoundsInRadius(Vector3.new(1,2,3),2)
assert(#radiusHits>=1)
local exclude=RaycastParams.new();exclude.FilterDescendantsInstances={folder}
assert(workspace:Raycast(Vector3.new(1,2,10),Vector3.new(0,0,-20),exclude)==nil)
assert(Vector3.new(3,4,0).Magnitude==5)
local color=Color3.fromRGB(255,64,0)
assert(typeof(color)=="Color3")
assert(color.R==1 and math.abs(color.G-64/255)<0.000001 and color.B==0)
assert(color:ToHex()=="FF4000")
local v2=Vector2.new(3,4)
assert(v2.Magnitude==5 and v2.Unit.X==0.6)
local cf=CFrame.new(4,5,6)*CFrame.Angles(0,math.pi/2,0)
assert(typeof(cf)=="CFrame" and cf.Position==Vector3.new(4,5,6))
local worldPoint=cf:PointToWorldSpace(Vector3.new(0,0,-2))
assert(math.abs(worldPoint.X-2)<0.00001 and math.abs(worldPoint.Z-6)<0.00001)
assert((cf:Inverse()*cf).Position.Magnitude<0.00001)
local size=UDim2.fromScale(0.5,0.25)
assert(size.X.Scale==0.5 and size.Y.Scale==0.25)
assert(Enum.Material.Grass.Name=="Grass")
assert(BrickColor.new("Bright red").Color.R>0.7)
local mixed=color:Lerp(Color3.new(0,0,1),0.5)
assert(math.abs(mixed.R-0.5)<0.000001 and mixed.B==0.5)
assert(game:GetService("RunService"):IsStudio())
assert(game:GetService("Workspace")==workspace)

local players=game:GetService("Players")
assert(players.LocalPlayer.UserId==1)
assert(players.LocalPlayer:FindFirstChild("HumanoidRootPart"))
local localPlayer=players.LocalPlayer

assert(typeof(players)=="Instance")
assert(players:FindFirstChild("LocalPlayer")==localPlayer)
assert(game:FindFirstChild("Workspace")==workspace)
assert(game:GetService("Lighting"):FindFirstChild("Missing")==nil)
assert(game:GetService("HttpService"):FindFirstChild("Anything")==nil)

local tagged=Instance.new("Part")
local tags=game:GetService("CollectionService")
tags:AddTag(tagged,"BuildPiece")
assert(tags:HasTag(tagged,"BuildPiece") and #tags:GetTagged("BuildPiece")==1)
local propertyChanged=false
tagged:GetPropertyChangedSignal("Anchored"):Connect(function() propertyChanged=true end)
tagged.Anchored=false
assert(propertyChanged==true)
local tween=game:GetService("TweenService"):Create(tagged,nil,{Transparency=0.5})
tween:Play()
assert(tagged.Transparency==0.5)
local model=Instance.new("Model")
local modelPart=Instance.new("Part");modelPart.Parent=model
model:PivotTo(CFrame.new(10,0,0))
assert(math.abs(modelPart.Position.X-10)<0.00001)
local store=game:GetService("DataStoreService"):GetDataStore("Smoke")
store:SetAsync("score",25)
assert(store:GetAsync("score")==25)

local remote=Instance.new("RemoteEvent")
local remoteSeen=false
remote.OnServerEvent:Connect(function(player,value) remoteSeen=(player==players.LocalPlayer and value=="hello") end)
remote:FireServer("hello")
assert(remoteSeen)
local bindable=Instance.new("BindableEvent")
local bindableSeen=false
bindable.Event:Connect(function(value) bindableSeen=(value==42) end)
bindable:Fire(42)
assert(bindableSeen)
local remoteFunction=Instance.new("RemoteFunction")
remoteFunction.OnServerInvoke=function(player,value) return player.UserId+value end
assert(remoteFunction:InvokeServer(8)==9)
local tweenInfo=TweenInfo.new(0.25,Enum.EasingStyle.Linear,Enum.EasingDirection.Out)
assert(tweenInfo.Time==0.25)
assert(NumberSequence.new(0.5).Keypoints[1].Value==0.5)
assert(ColorSequence.new(Color3.new(1,0,0)).Keypoints[2].Value.R==1)
local sharedStoreA=game:GetService("DataStoreService"):GetDataStore("Shared")
sharedStoreA:SetAsync("x",77)
local sharedStoreB=game:GetService("DataStoreService"):GetDataStore("Shared")
assert(sharedStoreB:GetAsync("x")==77)
local fired=false
folder.ChildAdded:Connect(function(child) fired=child.Name=="AddedChild" end)
local child=Instance.new("Folder")
child.Name="AddedChild"
child.Parent=folder
assert(fired)
print("ROBLOX_COMPAT_SMOKE_OK",part:GetFullName())
