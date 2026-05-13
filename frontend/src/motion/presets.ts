export const motionEase = [0.16, 1, 0.3, 1] as const

export const staggerDelay = (
  index = 0,
  baseDelay = 0,
  step = 0.035,
  maxIndex = 8
) => baseDelay + Math.min(index, maxIndex) * step

export const inViewOnce = {
  once: true,
  margin: '0px 0px -80px 0px' as const,
  amount: 0.16
} as const

export const revealInitial = {
  opacity: 0,
  y: 24
}

export const revealInView = {
  opacity: 1,
  y: 0
}

export const cardInitial = {
  opacity: 0,
  y: 34,
  scale: 0.98
}

export const cardInView = {
  opacity: 1,
  y: 0,
  scale: 1
}

export const cardExit = {
  opacity: 0,
  y: 22,
  scale: 0.98
}

export const revealTransition = {
  duration: 0.42,
  ease: motionEase
}

export const cardTransition = (index = 0, baseDelay = 0) => ({
  duration: 0.38,
  delay: staggerDelay(index, baseDelay),
  ease: motionEase
})

export const softSpring = {
  type: 'spring' as const,
  stiffness: 420,
  damping: 34
}

export const pageInitial = {
  opacity: 0,
  y: 12
}

export const pageAnimate = {
  opacity: 1,
  y: 0
}

export const pageExit = {
  opacity: 0,
  y: -8
}

export const pageTransition = {
  duration: 0.24,
  ease: motionEase
}

export const navShellInitial = {
  y: -18,
  opacity: 0,
  scale: 0.98
}

export const navShellAnimate = {
  y: 0,
  opacity: 1,
  scale: 1
}

export const navShellTransition = {
  duration: 0.55,
  ease: motionEase
}

export const navItemHover = {
  y: -2,
  scale: 1.04
}

export const navItemPress = {
  scale: 0.96
}

export const subtleButtonHover = {
  y: -1,
  scale: 1.04
}

export const subtleButtonPress = {
  scale: 0.94
}

export const primaryActionHover = {
  y: -3,
  scale: 1.03
}

export const primaryActionPress = {
  scale: 0.98
}

export const mobileMenuInitial = {
  opacity: 0,
  y: -14,
  scale: 0.96
}

export const mobileMenuAnimate = {
  opacity: 1,
  y: 0,
  scale: 1
}

export const mobileMenuExit = {
  opacity: 0,
  y: -10,
  scale: 0.97
}

export const mobileMenuTransition = {
  duration: 0.32,
  ease: motionEase
}

export const mobileMenuItemInitial = {
  opacity: 0,
  x: -10
}

export const mobileMenuItemAnimate = {
  opacity: 1,
  x: 0
}

export const mobileMenuItemTransition = (index = 0) => ({
  delay: staggerDelay(index),
  duration: 0.28,
  ease: motionEase
})

export const progressBarInitial = {
  opacity: 0,
  scaleX: 0
}

export const progressBarAnimate = {
  opacity: 1,
  scaleX: 1
}

export const progressBarExit = {
  opacity: 0,
  scaleX: 1
}

export const progressBarTransition = {
  duration: 0.32,
  ease: motionEase
}

export const modalRootInitial = {
  opacity: 0
}

export const modalRootAnimate = {
  opacity: 1
}

export const modalRootExit = {
  opacity: 0
}

export const modalBackdropTransition = {
  duration: 0.24,
  ease: motionEase
}

export const modalPanelInitial = {
  opacity: 0,
  y: 18,
  scale: 0.96
}

export const modalPanelAnimate = {
  opacity: 1,
  y: 0,
  scale: 1
}

export const modalPanelExit = {
  opacity: 0,
  y: 10,
  scale: 0.98
}

export const modalPanelTransition = {
  duration: 0.32,
  ease: motionEase
}

export const heroContentInitial = {
  opacity: 0,
  y: 24
}

export const heroContentAnimate = {
  opacity: 1,
  y: 0
}

export const heroContentExit = {
  opacity: 0,
  y: -14
}

export const heroContentTransition = {
  duration: 0.46,
  ease: motionEase
}

export const heroItemInitial = {
  opacity: 0,
  y: 24,
  scale: 0.98
}

export const heroItemAnimate = {
  opacity: 1,
  y: 0,
  scale: 1
}

export const heroItemTransition = (delay = 0) => ({
  duration: 0.7,
  delay,
  ease: motionEase
})

export const authPageInitial = {
  opacity: 0
}

export const authPageAnimate = {
  opacity: 1
}

export const authPageTransition = {
  duration: 0.45,
  ease: motionEase
}

export const authOverlayTransition = {
  duration: 0.6,
  ease: motionEase
}

export const authCardInitial = {
  opacity: 0,
  y: 28,
  scale: 0.96
}

export const authCardAnimate = {
  opacity: 1,
  y: 0,
  scale: 1
}

export const authCardTransition = {
  duration: 0.62,
  ease: motionEase
}

export const authItemInitial = {
  opacity: 0,
  y: 18
}

export const authItemAnimate = {
  opacity: 1,
  y: 0
}

export const authItemTransition = (delay = 0) => ({
  duration: 0.46,
  delay,
  ease: motionEase
})

export const authSubmitHover = {
  y: -2,
  scale: 1.01
}

export const authSubmitPress = {
  scale: 0.98
}

export const motionBlockVariants = {
  reveal: {
    initial: revealInitial,
    animate: revealInView,
    transition: (index = 0, delay = 0) => ({
      duration: 0.34,
      delay: staggerDelay(index, delay),
      ease: motionEase
    })
  },
  card: {
    initial: cardInitial,
    animate: cardInView,
    transition: cardTransition
  },
  hero: {
    initial: heroItemInitial,
    animate: heroItemAnimate,
    transition: (_index = 0, delay = 0) => heroItemTransition(delay)
  },
  panel: {
    initial: {
      opacity: 0,
      y: 18,
      scale: 0.995
    },
    animate: {
      opacity: 1,
      y: 0,
      scale: 1
    },
    transition: (index = 0, delay = 0) => ({
      duration: 0.36,
      delay: staggerDelay(index, delay),
      ease: motionEase
    })
  }
} as const
