const app = getApp();
const { request } = require('../../utils/request');

Page({
  data: {
    patients: [],
    currentPatientId: null
  },

  onShow() {
    const currentPatient = wx.getStorageSync('currentPatient');
    this.setData({
      currentPatientId: currentPatient ? currentPatient.id : null
    });
    this.loadPatients();
  },

  async loadPatients() {
    try {
      const result = await request({
        url: '/api/patient/list',
        method: 'GET'
      });
      const patients = result.data || [];
      this.setData({ patients });
      if (!this.data.currentPatientId && patients.length) {
        this.setCurrentPatient(patients[0]);
      }
    } catch (error) {
      console.error('加载患者档案失败', error);
    }
  },

  onSelect(e) {
    const id = Number(e.currentTarget.dataset.id);
    const patient = this.data.patients.find(item => item.id === id);
    if (!patient) return;
    this.setCurrentPatient(patient);
    wx.showToast({ title: '已切换当前患者', icon: 'success' });
  },

  setCurrentPatient(patient) {
    wx.setStorageSync('currentPatient', patient);
    app.globalData.currentPatient = patient;
    this.setData({ currentPatientId: patient.id });
  },

  onCreate() {
    wx.navigateTo({ url: '/pages/upload/index?mode=create' });
  },

  onEdit(e) {
    const id = Number(e.currentTarget.dataset.id);
    wx.navigateTo({ url: `/pages/upload/index?mode=edit&id=${id}` });
  },

  onDelete(e) {
    const id = Number(e.currentTarget.dataset.id);
    const patient = this.data.patients.find(item => item.id === id);
    if (!patient) return;
    wx.showModal({
      title: '确认删除',
      content: `确定删除患者档案【${patient.patientName}】吗？`,
      success: async (res) => {
        if (!res.confirm) return;
        try {
          await request({
            url: `/api/patient/${id}`,
            method: 'DELETE'
          });
          if (this.data.currentPatientId === id) {
            wx.removeStorageSync('currentPatient');
            app.globalData.currentPatient = null;
          }
          wx.showToast({ title: '删除成功', icon: 'success' });
          this.loadPatients();
        } catch (error) {
          console.error('删除患者档案失败', error);
        }
      }
    });
  }
});
