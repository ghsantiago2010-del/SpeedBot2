/**
 * Web Audio synthesizer for robot horn, button beeps, and motor rumble
 */
class SoundEffects {
  private ctx: AudioContext | null = null;
  private hornOsc1: OscillatorNode | null = null;
  private hornOsc2: OscillatorNode | null = null;
  private hornGain: GainNode | null = null;
  private isHornPlaying = false;

  private initCtx() {
    if (!this.ctx && typeof window !== 'undefined') {
      const AudioCtx = window.AudioContext || (window as any).webkitAudioContext;
      if (AudioCtx) {
        this.ctx = new AudioCtx();
      }
    }
    if (this.ctx && this.ctx.state === 'suspended') {
      this.ctx.resume();
    }
  }

  /**
   * Start playing the horn continuously while the user holds the button
   */
  public startHorn() {
    this.initCtx();
    if (!this.ctx || this.isHornPlaying) return;

    try {
      this.isHornPlaying = true;
      this.hornOsc1 = this.ctx.createOscillator();
      this.hornOsc2 = this.ctx.createOscillator();
      this.hornGain = this.ctx.createGain();

      this.hornOsc1.type = 'sawtooth';
      this.hornOsc2.type = 'square';
      this.hornOsc1.frequency.setValueAtTime(420, this.ctx.currentTime);
      this.hornOsc2.frequency.setValueAtTime(435, this.ctx.currentTime);

      this.hornGain.gain.setValueAtTime(0.01, this.ctx.currentTime);
      this.hornGain.gain.linearRampToValueAtTime(0.25, this.ctx.currentTime + 0.05);

      this.hornOsc1.connect(this.hornGain);
      this.hornOsc2.connect(this.hornGain);
      this.hornGain.connect(this.ctx.destination);

      this.hornOsc1.start();
      this.hornOsc2.start();
    } catch {
      // Audio policy
    }
  }

  /**
   * Stop the horn sound as soon as the user releases the button
   */
  public stopHorn() {
    if (!this.ctx || !this.isHornPlaying) return;

    try {
      if (this.hornGain) {
        this.hornGain.gain.setValueAtTime(this.hornGain.gain.value, this.ctx.currentTime);
        this.hornGain.gain.exponentialRampToValueAtTime(0.001, this.ctx.currentTime + 0.08);
      }
      setTimeout(() => {
        try {
          this.hornOsc1?.stop();
          this.hornOsc2?.stop();
          this.hornOsc1?.disconnect();
          this.hornOsc2?.disconnect();
          this.hornGain?.disconnect();
        } catch {
          // ignore
        }
        this.hornOsc1 = null;
        this.hornOsc2 = null;
        this.hornGain = null;
        this.isHornPlaying = false;
      }, 90);
    } catch {
      this.isHornPlaying = false;
    }
  }

  public playHorn() {
    this.startHorn();
    setTimeout(() => {
      this.stopHorn();
    }, 350);
  }

  public playBeep(freq = 880, duration = 0.1) {
    this.initCtx();
    if (!this.ctx) return;

    try {
      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();

      osc.type = 'sine';
      osc.frequency.setValueAtTime(freq, this.ctx.currentTime);
      gain.gain.setValueAtTime(0.15, this.ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.01, this.ctx.currentTime + duration);

      osc.connect(gain);
      gain.connect(this.ctx.destination);

      osc.start();
      osc.stop(this.ctx.currentTime + duration);
    } catch {
      // ignore
    }
  }

  public playWarning() {
    this.initCtx();
    if (!this.ctx) return;

    try {
      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();

      osc.type = 'triangle';
      osc.frequency.setValueAtTime(300, this.ctx.currentTime);
      osc.frequency.linearRampToValueAtTime(150, this.ctx.currentTime + 0.6);

      gain.gain.setValueAtTime(0.3, this.ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.01, this.ctx.currentTime + 0.6);

      osc.connect(gain);
      gain.connect(this.ctx.destination);

      osc.start();
      osc.stop(this.ctx.currentTime + 0.6);
    } catch {
      // ignore
    }
  }
}

export const soundEffects = new SoundEffects();
